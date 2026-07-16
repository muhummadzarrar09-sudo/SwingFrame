package app.swingframe.model

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.Player
import app.swingframe.annotation.AnnotationShape
import app.swingframe.annotation.AnnotationTool
import app.swingframe.export.ExportStatus
import app.swingframe.project.FrameBookmark
import app.swingframe.project.LocalProject
import app.swingframe.project.RelinkConflict

enum class AppStage {
    HOME,
    PROBING,
    PREVIEW,
    INDEXING,
    VIEWER,
}

data class VideoMetadata(
    val durationUs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val declaredFps: Float?,
    val mimeType: String,
    val codecLabel: String,
    val estimatedFrameCount: Int?,
    val bitrate: Long?,
    val sourceSizeBytes: Long?,
    val sourceLastModifiedEpochMs: Long?,
    val sourceFingerprint: String,
)

data class VideoSource(
    val uri: Uri,
    val displayName: String,
    val metadata: VideoMetadata,
)

data class IndexedVideo(
    val source: VideoSource,
    val frameTimestampsUs: List<Long>,
    val detectedFps: Float,
    val isVariableFrameRate: Boolean,
) {
    val totalFrames: Int get() = frameTimestampsUs.size
}

data class TimelineThumbnail(
    val frameIndex: Int,
    val bitmap: Bitmap,
)

data class SwingFrameUiState(
    val stage: AppStage = AppStage.HOME,
    val source: VideoSource? = null,
    val video: IndexedVideo? = null,
    val recentProjects: List<LocalProject> = emptyList(),
    val activeProjectId: String? = null,
    val bookmarks: List<FrameBookmark> = emptyList(),
    val relinkConflict: RelinkConflict? = null,
    val loadingProgress: Float = 0f,
    val loadingLabel: String = "",
    /** Timeline/playhead target requested by the user. */
    val currentFrameIndex: Int = 0,
    /** Exact frame identity represented by [currentBitmap], or null while unresolved. */
    val resolvedFrameIndex: Int? = null,
    val currentBitmap: Bitmap? = null,
    val timelineThumbnails: List<TimelineThumbnail> = emptyList(),
    val isFrameLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 0.25f,
    val annotationTool: AnnotationTool = AnnotationTool.SELECT,
    val annotationColorArgb: Int = 0xFFE5A63B.toInt(),
    val annotationStrokeWidthDp: Float = 3f,
    val currentFrameAnnotations: List<AnnotationShape> = emptyList(),
    val carriedAnnotations: List<AnnotationShape> = emptyList(),
    val selectedAnnotationId: String? = null,
    val annotationOverlayVisible: Boolean = true,
    val carryForwardEnabled: Boolean = false,
    val canUndoAnnotations: Boolean = false,
    val canRedoAnnotations: Boolean = false,
    val exportStatus: ExportStatus = ExportStatus(),
    val errorMessage: String? = null,
)
