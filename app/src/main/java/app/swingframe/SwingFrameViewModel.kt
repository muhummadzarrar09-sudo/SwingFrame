package app.swingframe

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import app.swingframe.annotation.AnnotationSession
import app.swingframe.annotation.AnnotationShape
import app.swingframe.annotation.AnnotationStore
import app.swingframe.annotation.AnnotationStyle
import app.swingframe.annotation.AnnotationTool
import app.swingframe.data.FrameIndexer
import app.swingframe.data.VideoProbe
import app.swingframe.media.MediaSessionController
import app.swingframe.export.ExportManager
import app.swingframe.export.ExportStatus
import app.swingframe.export.ExportType
import app.swingframe.export.VideoExportRequest
import app.swingframe.model.AppStage
import app.swingframe.model.SwingFrameUiState
import app.swingframe.project.FrameBookmark
import app.swingframe.project.LocalProject
import app.swingframe.project.ProjectCompatibility
import app.swingframe.project.ProjectStore
import app.swingframe.project.RelinkConflict
import app.swingframe.project.SourceCompatibility
import app.swingframe.util.LocalDataCorruptionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
class SwingFrameViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SwingFrameUiState())
    val uiState: StateFlow<SwingFrameUiState> = _uiState.asStateFlow()

    private val probe = VideoProbe(application)
    private val indexer = FrameIndexer(application)
    private val mediaSession = MediaSessionController(application, viewModelScope)
    private val annotationStore = AnnotationStore(application)
    private val annotationSession = AnnotationSession(
        store = annotationStore,
        scope = viewModelScope,
        onPersistenceError = {
            _uiState.update { state ->
                state.copy(errorMessage = "Annotations could not be saved. Check available storage before leaving this project.")
            }
        },
    )
    private val projectStore = ProjectStore(application)
    private val exportManager = ExportManager(application)

    private var analysisJob: Job? = null
    private var projectSaveJob: Job? = null
    private var exportJob: Job? = null

    private var localProjects: List<LocalProject> = emptyList()
    private val projectIndexReady = CompletableDeferred<Unit>()
    private var projectIndexLoaded = false
    private var activeProjectId: String? = null
    private var pendingResumeProject: LocalProject? = null
    private var pendingRelinkProject: LocalProject? = null

    init {
        viewModelScope.launch {
            var loadError: String? = null
            try {
                localProjects = projectStore.load()
                try {
                    annotationStore.cleanupOrphans(localProjects.map { Uri.parse(it.sourceUri) })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    loadError = "Projects loaded, but old private annotation files could not be cleaned up."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                localProjects = emptyList()
                loadError = error.message ?: "Local projects could not be loaded."
            } finally {
                projectIndexLoaded = true
                if (!projectIndexReady.isCompleted) projectIndexReady.complete(Unit)
            }
            _uiState.update {
                it.copy(recentProjects = localProjects, errorMessage = loadError ?: it.errorMessage)
            }
        }
        viewModelScope.launch {
            mediaSession.state.collect { media ->
                val video = media.video ?: return@collect
                val current = _uiState.value
                if (current.stage != AppStage.VIEWER || current.video !== video) return@collect
                val annotationFrame = annotationSession.snapshot(
                    media.requestedFrameIndex,
                    current.carryForwardEnabled,
                )
                _uiState.update {
                    it.copy(
                        currentFrameIndex = media.requestedFrameIndex,
                        resolvedFrameIndex = media.resolvedFrameIndex,
                        currentBitmap = media.bitmap,
                        timelineThumbnails = media.thumbnails,
                        isFrameLoading = media.isLoading,
                        isPlaying = media.isPlaying,
                        currentFrameAnnotations = annotationFrame.current,
                        carriedAnnotations = annotationFrame.carried,
                        selectedAnnotationId = if (it.currentFrameIndex == media.requestedFrameIndex) it.selectedAnnotationId else null,
                        canUndoAnnotations = annotationFrame.canUndo,
                        canRedoAnnotations = annotationFrame.canRedo,
                        errorMessage = media.errorMessage ?: it.errorMessage,
                    )
                }
                updateActiveProjectFrame(media.requestedFrameIndex)
            }
        }
    }

    fun openVideo(uri: Uri) {
        pendingResumeProject = null
        pendingRelinkProject = null
        openVideoInternal(uri, autoAnalyze = false)
    }

    fun openRecentProject(project: LocalProject) {
        pendingRelinkProject = null
        pendingResumeProject = project
        openVideoInternal(Uri.parse(project.sourceUri), autoAnalyze = true)
    }

    fun relinkProject(project: LocalProject, newUri: Uri) {
        pendingRelinkProject = project
        pendingResumeProject = project
        openVideoInternal(newUri, autoAnalyze = false)
    }

    fun confirmRelinkConflict() {
        val conflict = _uiState.value.relinkConflict ?: return
        val project = localProjects.firstOrNull { it.id == conflict.projectId } ?: return
        val source = _uiState.value.source ?: return
        _uiState.update { it.copy(relinkConflict = null) }
        viewModelScope.launch {
            try {
                finalizeRelink(project, source)
                analyzeVideo()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                pendingRelinkProject = null
                pendingResumeProject = null
                _uiState.update { it.copy(errorMessage = error.toUserMessage()) }
            }
        }
    }

    fun cancelRelinkConflict() {
        pendingRelinkProject = null
        pendingResumeProject = null
        returnHome()
    }

    fun deleteRecentProject(project: LocalProject) {
        projectSaveJob?.cancel()
        projectSaveJob = null
        localProjects = localProjects.filterNot { it.id == project.id }
        val snapshot = localProjects
        _uiState.update { it.copy(recentProjects = snapshot) }
        viewModelScope.launch {
            try {
                // Persist the index first. Otherwise a failed index write can resurrect a project
                // whose annotation file has already been deleted.
                projectStore.save(snapshot)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                replaceProject(project)
                _uiState.update {
                    it.copy(
                        recentProjects = localProjects,
                        errorMessage = "SwingFrame could not delete this project from local storage.",
                    )
                }
                return@launch
            }

            // Annotation files are currently keyed by source URI. A second project can point to
            // the same URI after relinking, so only remove shared vectors with the final owner.
            if (snapshot.any { it.sourceUri == project.sourceUri }) return@launch
            try {
                annotationStore.delete(Uri.parse(project.sourceUri))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // The project index is already safely deleted. A leftover app-private vector
                // file is harmless and may be cleaned by a later maintenance pass.
                _uiState.update { state ->
                    state.copy(errorMessage = "Project deleted, but some app-private cleanup could not be completed.")
                }
            }
            releasePersistedReadGrant(Uri.parse(project.sourceUri))
        }
    }

    private fun openVideoInternal(uri: Uri, autoAnalyze: Boolean) {
        releaseCurrentVideo()
        analysisJob = viewModelScope.launch {
            _uiState.value = SwingFrameUiState(
                stage = AppStage.PROBING,
                recentProjects = localProjects,
                loadingProgress = 0.12f,
                loadingLabel = "Reading source",
            )
            try {
                val source = probe.probe(uri)
                var shouldAutoAnalyze = autoAnalyze
                val relinkProject = pendingRelinkProject
                val compatibilityProject = relinkProject ?: pendingResumeProject
                val conflict = if (compatibilityProject != null &&
                    ProjectCompatibility.compare(
                        compatibilityProject,
                        source.metadata.sourceFingerprint,
                    ) == SourceCompatibility.MISMATCH
                ) {
                    RelinkConflict(
                        projectId = compatibilityProject.id,
                        projectName = compatibilityProject.displayName,
                        replacementUri = source.uri.toString(),
                        replacementName = source.displayName,
                        expectedFingerprint = compatibilityProject.sourceFingerprint,
                        actualFingerprint = source.metadata.sourceFingerprint,
                    )
                } else null

                if (relinkProject != null && conflict == null) {
                    finalizeRelink(relinkProject, source)
                    shouldAutoAnalyze = true
                }

                _uiState.value = SwingFrameUiState(
                    stage = AppStage.PREVIEW,
                    source = source,
                    recentProjects = localProjects,
                    relinkConflict = conflict,
                )
                if (shouldAutoAnalyze && conflict == null) {
                    viewModelScope.launch {
                        delay(1L)
                        analyzeVideo()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                returnHomeWithError(error.toUserMessage())
            }
        }
    }

    fun analyzeVideo() {
        val source = _uiState.value.source ?: return
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = AppStage.INDEXING,
                    loadingProgress = 0.01f,
                    loadingLabel = "Building exact timeline",
                    errorMessage = null,
                )
            }
            try {
                val indexedVideo = indexer.index(source) { progress ->
                    _uiState.update { current ->
                        if (current.stage == AppStage.INDEXING) {
                            current.copy(loadingProgress = progress)
                        } else {
                            current
                        }
                    }
                }

                projectIndexReady.await()
                val now = System.currentTimeMillis()
                val resumed = pendingResumeProject
                    ?: localProjects.firstOrNull { it.sourceUri == source.uri.toString() }
                val project = if (resumed == null) {
                    LocalProject(
                        sourceUri = source.uri.toString(),
                        displayName = source.displayName,
                        durationUs = source.metadata.durationUs,
                        width = source.metadata.width,
                        height = source.metadata.height,
                        rotationDegrees = source.metadata.rotationDegrees,
                        sourceSizeBytes = source.metadata.sourceSizeBytes,
                        sourceLastModifiedEpochMs = source.metadata.sourceLastModifiedEpochMs,
                        sourceFingerprint = source.metadata.sourceFingerprint,
                        totalFrames = indexedVideo.totalFrames,
                        lastFrameIndex = 0,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    )
                } else {
                    resumed.copy(
                        sourceUri = source.uri.toString(),
                        displayName = source.displayName,
                        durationUs = source.metadata.durationUs,
                        width = source.metadata.width,
                        height = source.metadata.height,
                        rotationDegrees = source.metadata.rotationDegrees,
                        sourceSizeBytes = source.metadata.sourceSizeBytes,
                        sourceLastModifiedEpochMs = source.metadata.sourceLastModifiedEpochMs,
                        sourceFingerprint = source.metadata.sourceFingerprint,
                        totalFrames = indexedVideo.totalFrames,
                        lastFrameIndex = resumed.lastFrameIndex.coerceIn(0, indexedVideo.totalFrames - 1),
                        updatedAtEpochMs = now,
                        bookmarks = resumed.bookmarks.filter { it.frameIndex in 0 until indexedVideo.totalFrames },
                    )
                }
                activeProjectId = project.id
                replaceProject(project)
                scheduleProjectSave()
                pendingResumeProject = null

                annotationSession.open(source.uri)

                val initialFrame = project.lastFrameIndex.coerceIn(0, indexedVideo.totalFrames - 1)
                val annotationFrame = annotationSession.snapshot(initialFrame, carryForward = false)
                _uiState.value = SwingFrameUiState(
                    stage = AppStage.VIEWER,
                    source = source,
                    video = indexedVideo,
                    recentProjects = localProjects,
                    activeProjectId = project.id,
                    bookmarks = project.bookmarks.sortedBy { it.frameIndex },
                    currentFrameIndex = initialFrame,
                    currentFrameAnnotations = annotationFrame.current,
                    canUndoAnnotations = annotationFrame.canUndo,
                    canRedoAnnotations = annotationFrame.canRedo,
                    isFrameLoading = true,
                )

                mediaSession.start(indexedVideo, initialFrame)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mediaSession.release()
                _uiState.update {
                    it.copy(
                        stage = AppStage.PREVIEW,
                        loadingProgress = 0f,
                        errorMessage = error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun seekTo(frameIndex: Int) {
        mediaSession.request(frameIndex)
    }

    fun stepBy(delta: Int) {
        mediaSession.step(delta)
    }

    fun togglePlayback() {
        mediaSession.togglePlayback(_uiState.value.playbackSpeed)
    }

    fun pausePlayback() {
        mediaSession.stopPlayback()
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.1f, 1f)
        _uiState.update { it.copy(playbackSpeed = safeSpeed) }
        mediaSession.updatePlaybackSpeed(safeSpeed)
    }

    fun addBookmark(label: String) {
        val cleanLabel = label.trim().take(40)
        if (cleanLabel.isBlank()) return
        val project = activeProject() ?: return
        val frameIndex = _uiState.value.currentFrameIndex
        val existing = project.bookmarks.firstOrNull { it.frameIndex == frameIndex }
        val bookmark = existing?.copy(label = cleanLabel)
            ?: FrameBookmark(frameIndex = frameIndex, label = cleanLabel)
        val bookmarks = (project.bookmarks.filterNot { it.frameIndex == frameIndex } + bookmark)
            .sortedBy { it.frameIndex }
        val updated = project.copy(
            bookmarks = bookmarks,
            lastFrameIndex = frameIndex,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        replaceProject(updated)
        _uiState.update { it.copy(bookmarks = bookmarks, recentProjects = localProjects) }
        scheduleProjectSave()
    }

    fun deleteBookmark(bookmarkId: String) {
        val project = activeProject() ?: return
        val bookmarks = project.bookmarks.filterNot { it.id == bookmarkId }
        replaceProject(project.copy(bookmarks = bookmarks, updatedAtEpochMs = System.currentTimeMillis()))
        _uiState.update { it.copy(bookmarks = bookmarks, recentProjects = localProjects) }
        scheduleProjectSave()
    }

    fun jumpToBookmark(frameIndex: Int) {
        seekTo(frameIndex)
    }

    fun exportCurrentFrame(burnAnnotations: Boolean) {
        val state = _uiState.value
        val source = state.source ?: return
        val video = state.video ?: return
        if (state.exportStatus.isExporting) return
        val frameIndex = state.currentFrameIndex.coerceIn(0, video.totalFrames - 1)
        pausePreviewDecodingForExport()
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                exportStatus = ExportStatus(
                    isExporting = true,
                    type = ExportType.STILL,
                    progressPercent = 10,
                    message = "Decoding source frame",
                ),
            )
        }
        exportJob = viewModelScope.launch {
            try {
                val annotationSnapshot = annotationSession.snapshot(frameIndex, state.carryForwardEnabled)
                val uri = exportManager.exportStill(
                    sourceUri = source.uri,
                    timestampUs = video.frameTimestampsUs[frameIndex],
                    annotations = annotationSnapshot.current,
                    carriedAnnotations = annotationSnapshot.carried,
                    burnAnnotations = burnAnnotations,
                )
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus(
                            type = ExportType.STILL,
                            progressPercent = 100,
                            message = "Saved to Pictures / SwingFrame",
                            savedUri = uri,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus(
                            type = ExportType.STILL,
                            errorMessage = error.toUserMessage(),
                        ),
                    )
                }
            } finally {
                resumePreviewDecodingAfterExport()
            }
        }
    }

    fun exportVideoRange(
        startFrameIndex: Int,
        endFrameIndex: Int,
        playbackSpeed: Float,
        burnAnnotations: Boolean,
    ) {
        val state = _uiState.value
        val source = state.source ?: return
        val video = state.video ?: return
        if (state.exportStatus.isExporting) return
        val start = minOf(startFrameIndex, endFrameIndex).coerceIn(0, video.totalFrames - 1)
        val end = maxOf(startFrameIndex, endFrameIndex).coerceIn(start, video.totalFrames - 1)
        val snapshot = annotationSession.allFrames()
        pausePreviewDecodingForExport()
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                exportStatus = ExportStatus(
                    isExporting = true,
                    type = ExportType.VIDEO,
                    progressPercent = 0,
                    message = "Preparing video export",
                ),
            )
        }
        exportJob = viewModelScope.launch {
            try {
                val uri = exportManager.exportVideo(
                    request = VideoExportRequest(
                        sourceUri = source.uri,
                        frameTimestampsUs = video.frameTimestampsUs,
                        startFrameIndex = start,
                        endFrameIndex = end,
                        playbackSpeed = playbackSpeed,
                        burnAnnotations = burnAnnotations,
                        carryForward = state.carryForwardEnabled,
                        annotationsByFrame = snapshot,
                    ),
                    onProgress = { progress ->
                        _uiState.update { current ->
                            current.copy(
                                exportStatus = current.exportStatus.copy(
                                    isExporting = true,
                                    progressPercent = progress,
                                    message = "Rendering local video",
                                ),
                            )
                        }
                    },
                )
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus(
                            type = ExportType.VIDEO,
                            progressPercent = 100,
                            message = "Saved to Movies / SwingFrame",
                            savedUri = uri,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus(
                            type = ExportType.VIDEO,
                            errorMessage = error.toUserMessage(),
                        ),
                    )
                }
            } finally {
                resumePreviewDecodingAfterExport()
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _uiState.update { it.copy(exportStatus = ExportStatus(message = "Export cancelled")) }
    }

    fun dismissExportStatus() {
        if (!_uiState.value.exportStatus.isExporting) {
            _uiState.update { it.copy(exportStatus = ExportStatus()) }
        }
    }

    fun selectAnnotationTool(tool: AnnotationTool) {
        _uiState.update {
            it.copy(
                annotationTool = tool,
                selectedAnnotationId = if (tool == AnnotationTool.SELECT) it.selectedAnnotationId else null,
            )
        }
    }

    fun selectAnnotation(annotationId: String?) {
        _uiState.update { it.copy(selectedAnnotationId = annotationId) }
    }

    fun setAnnotationColor(colorArgb: Int) {
        val selected = _uiState.value.selectedAnnotationId
        _uiState.update { it.copy(annotationColorArgb = colorArgb) }
        if (selected != null) {
            updateSelectedStyle { it.copy(colorArgb = colorArgb) }
        }
    }

    fun setAnnotationStrokeWidth(strokeWidthDp: Float) {
        val safeWidth = strokeWidthDp.coerceIn(1f, 12f)
        val selected = _uiState.value.selectedAnnotationId
        _uiState.update { it.copy(annotationStrokeWidthDp = safeWidth) }
        if (selected != null) {
            updateSelectedStyle { it.copy(strokeWidthDp = safeWidth) }
        }
    }

    fun toggleAnnotationOverlay() {
        _uiState.update { it.copy(annotationOverlayVisible = !it.annotationOverlayVisible) }
    }

    fun toggleCarryForward() {
        _uiState.update { current ->
            val enabled = !current.carryForwardEnabled
            val frame = annotationSession.snapshot(current.currentFrameIndex, enabled)
            current.copy(carryForwardEnabled = enabled, carriedAnnotations = frame.carried)
        }
    }

    fun addAnnotation(shape: AnnotationShape) {
        val frameIndex = _uiState.value.currentFrameIndex
        applyAnnotationSnapshot(annotationSession.add(frameIndex, shape), selectedId = shape.id)
    }

    fun replaceAnnotation(shape: AnnotationShape) {
        val frameIndex = _uiState.value.currentFrameIndex
        applyAnnotationSnapshot(annotationSession.replace(frameIndex, shape), selectedId = shape.id)
    }

    fun deleteSelectedAnnotation() {
        val selected = _uiState.value.selectedAnnotationId ?: return
        val frameIndex = _uiState.value.currentFrameIndex
        applyAnnotationSnapshot(annotationSession.delete(frameIndex, selected), selectedId = null)
    }

    fun clearCurrentFrameAnnotations() {
        val frameIndex = _uiState.value.currentFrameIndex
        if (!annotationSession.hasAnnotations(frameIndex)) return
        applyAnnotationSnapshot(annotationSession.clear(frameIndex), selectedId = null)
    }

    fun undoAnnotation() {
        val frameIndex = _uiState.value.currentFrameIndex
        applyAnnotationSnapshot(annotationSession.undo(frameIndex), selectedId = null)
    }

    fun redoAnnotation() {
        val frameIndex = _uiState.value.currentFrameIndex
        applyAnnotationSnapshot(annotationSession.redo(frameIndex), selectedId = null)
    }

    fun returnHome() {
        releaseCurrentVideo()
        _uiState.value = SwingFrameUiState(recentProjects = localProjects)
    }

    fun returnToPreview() {
        val source = _uiState.value.source
        analysisJob?.cancel()
        analysisJob = null
        annotationSession.close()
        flushProjectsAsync()
        activeProjectId = null
        exportJob?.cancel()
        exportJob = null
        mediaSession.release()
        _uiState.value = if (source != null) {
            SwingFrameUiState(stage = AppStage.PREVIEW, source = source, recentProjects = localProjects)
        } else {
            SwingFrameUiState(recentProjects = localProjects)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun pausePreviewDecodingForExport() {
        mediaSession.pauseForExport()
    }

    private fun resumePreviewDecodingAfterExport() {
        mediaSession.resumeAfterExport()
    }

    private suspend fun finalizeRelink(project: LocalProject, source: app.swingframe.model.VideoSource) {
        val oldUri = Uri.parse(project.sourceUri)
        val newUri = source.uri
        val newUriText = newUri.toString()
        val occupiedByAnotherProject = localProjects.any {
            it.id != project.id && it.sourceUri == newUriText
        }
        if (occupiedByAnotherProject) {
            throw IllegalArgumentException("That source is already attached to another local project.")
        }

        // Copy vectors before changing metadata, but retain the old copy until the project index is
        // safely committed. The previous implementation deleted first and could lose the project
        // after a failed index write.
        if (oldUri != newUri) annotationStore.copy(oldUri, newUri)
        val previousProjects = localProjects
        val updated = project.copy(
            sourceUri = newUriText,
            displayName = source.displayName,
            durationUs = source.metadata.durationUs,
            width = source.metadata.width,
            height = source.metadata.height,
            rotationDegrees = source.metadata.rotationDegrees,
            sourceSizeBytes = source.metadata.sourceSizeBytes,
            sourceLastModifiedEpochMs = source.metadata.sourceLastModifiedEpochMs,
            sourceFingerprint = source.metadata.sourceFingerprint,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        replaceProject(updated)
        try {
            persistProjectsNow()
        } catch (error: Throwable) {
            localProjects = previousProjects
            _uiState.update { it.copy(recentProjects = previousProjects) }
            throw error
        }

        if (oldUri != newUri && localProjects.none { it.sourceUri == oldUri.toString() }) {
            try {
                annotationStore.delete(oldUri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // The committed project points at the safe new copy; stale private data is harmless.
            }
            releasePersistedReadGrant(oldUri)
        }
        pendingResumeProject = updated
        pendingRelinkProject = null
    }

    private fun activeProject(): LocalProject? =
        activeProjectId?.let { id -> localProjects.firstOrNull { it.id == id } }

    private fun releasePersistedReadGrant(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some providers grant only transient access or have already revoked the grant.
        }
    }

    private fun replaceProject(project: LocalProject) {
        localProjects = (localProjects.filterNot { it.id == project.id } + project)
            .sortedByDescending { it.updatedAtEpochMs }
    }

    private fun updateActiveProjectFrame(frameIndex: Int) {
        val project = activeProject() ?: return
        if (project.lastFrameIndex == frameIndex) return
        replaceProject(
            project.copy(
                lastFrameIndex = frameIndex,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        _uiState.update { it.copy(recentProjects = localProjects) }
        scheduleProjectSave()
    }

    private fun scheduleProjectSave() {
        if (!projectIndexLoaded) return
        val snapshot = localProjects
        projectSaveJob?.cancel()
        projectSaveJob = viewModelScope.launch {
            delay(PROJECT_SAVE_DEBOUNCE_MS)
            try {
                projectStore.save(snapshot)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(errorMessage = "Project progress could not be saved. Check available storage.")
                }
            }
        }
    }

    private suspend fun persistProjectsNow() {
        projectSaveJob?.cancel()
        projectSaveJob = null
        projectStore.save(localProjects)
        _uiState.update { it.copy(recentProjects = localProjects) }
    }

    private fun flushProjectsAsync() {
        if (!projectIndexLoaded) return
        val snapshot = localProjects
        projectSaveJob?.cancel()
        projectSaveJob = null
        viewModelScope.launch {
            try {
                projectStore.save(snapshot)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(errorMessage = "Project progress could not be saved. Check available storage.")
                }
            }
        }
    }

    private fun updateSelectedStyle(transform: (AnnotationStyle) -> AnnotationStyle) {
        val selected = _uiState.value.selectedAnnotationId ?: return
        val frameIndex = _uiState.value.currentFrameIndex
        applyAnnotationSnapshot(
            annotationSession.updateStyle(frameIndex, selected, transform),
            selectedId = selected,
        )
    }

    private fun applyAnnotationSnapshot(
        snapshot: app.swingframe.annotation.AnnotationFrameSnapshot,
        selectedId: String?,
    ) {
        val carry = _uiState.value.carryForwardEnabled
        val complete = if (carry) {
            annotationSession.snapshot(_uiState.value.currentFrameIndex, carryForward = true)
        } else snapshot
        _uiState.update {
            it.copy(
                currentFrameAnnotations = complete.current,
                carriedAnnotations = complete.carried,
                selectedAnnotationId = selectedId,
                canUndoAnnotations = complete.canUndo,
                canRedoAnnotations = complete.canRedo,
            )
        }
    }

    private fun releaseCurrentVideo() {
        annotationSession.close()
        flushProjectsAsync()
        activeProjectId = null
        exportJob?.cancel()
        exportJob = null
        analysisJob?.cancel()
        analysisJob = null
        mediaSession.release()
    }

    private fun returnHomeWithError(message: String) {
        mediaSession.release()
        _uiState.value = SwingFrameUiState(
            stage = AppStage.HOME,
            recentProjects = localProjects,
            errorMessage = message,
        )
    }

    private fun Throwable.toUserMessage(): String {
        if (this is LocalDataCorruptionException) {
            return message ?: "Local project data was corrupt and has been quarantined."
        }
        val root = generateSequence(this) { it.cause }.last()
        return when (root) {
            is SecurityException -> "SwingFrame no longer has permission to read this video. Import it again."
            is IllegalArgumentException -> root.message ?: "The selected file is not a supported video."
            else -> root.message?.takeIf { it.isNotBlank() }
                ?: "The file may be corrupt or unsupported by this device."
        }
    }

    override fun onCleared() {
        releaseCurrentVideo()
        mediaSession.close()
        super.onCleared()
    }

    private companion object {
        const val PROJECT_SAVE_DEBOUNCE_MS = 650L
    }
}
