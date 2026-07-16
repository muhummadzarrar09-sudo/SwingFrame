package app.swingframe.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.swingframe.SwingFrameViewModel
import app.swingframe.model.AppStage
import app.swingframe.project.LocalProject

@Composable
fun SwingFrameApp(viewModel: SwingFrameViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingRelinkProject by remember { mutableStateOf<LocalProject?>(null) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val relink = pendingRelinkProject
            pendingRelinkProject = null
            if (relink != null) {
                viewModel.relinkProject(relink, uri)
            } else {
                viewModel.openVideo(uri)
            }
        }
    }
    val importVideo = {
        pendingRelinkProject = null
        picker.launch(arrayOf("video/*"))
    }
    val relinkVideo: (LocalProject) -> Unit = { project ->
        pendingRelinkProject = project
        picker.launch(arrayOf("video/*"))
    }

    when (state.stage) {
        AppStage.HOME -> HomeScreen(
            recentProjects = state.recentProjects,
            errorMessage = state.errorMessage,
            onImport = importVideo,
            onOpenProject = viewModel::openRecentProject,
            onRelinkProject = relinkVideo,
            onDeleteProject = viewModel::deleteRecentProject,
            onDismissError = viewModel::dismissError,
        )

        AppStage.PROBING -> LoadingScreen(
            label = state.loadingLabel,
            progress = null,
            onCancel = viewModel::returnHome,
        )

        AppStage.PREVIEW -> state.source?.let { source ->
            ImportPreviewScreen(
                source = source,
                errorMessage = state.errorMessage,
                onAnalyze = viewModel::analyzeVideo,
                onChooseAnother = importVideo,
                onBack = viewModel::returnHome,
                onDismissError = viewModel::dismissError,
            )
        }

        AppStage.INDEXING -> LoadingScreen(
            label = state.loadingLabel,
            progress = state.loadingProgress,
            detail = state.source?.displayName,
            onCancel = viewModel::returnToPreview,
        )

        AppStage.VIEWER -> state.video?.let { video ->
            FrameViewerScreen(
                state = state,
                video = video,
                onBack = viewModel::returnToPreview,
                onImportAnother = importVideo,
                onSeek = viewModel::seekTo,
                onStep = viewModel::stepBy,
                onTogglePlayback = viewModel::togglePlayback,
                onSpeedChange = viewModel::setPlaybackSpeed,
                onAddBookmark = viewModel::addBookmark,
                onDeleteBookmark = viewModel::deleteBookmark,
                onJumpToBookmark = viewModel::jumpToBookmark,
                onExportStill = viewModel::exportCurrentFrame,
                onExportVideo = viewModel::exportVideoRange,
                onCancelExport = viewModel::cancelExport,
                onDismissExportStatus = viewModel::dismissExportStatus,
                onSelectAnnotationTool = viewModel::selectAnnotationTool,
                onSelectAnnotation = viewModel::selectAnnotation,
                onAddAnnotation = viewModel::addAnnotation,
                onReplaceAnnotation = viewModel::replaceAnnotation,
                onSetAnnotationColor = viewModel::setAnnotationColor,
                onSetAnnotationStrokeWidth = viewModel::setAnnotationStrokeWidth,
                onToggleAnnotationOverlay = viewModel::toggleAnnotationOverlay,
                onToggleCarryForward = viewModel::toggleCarryForward,
                onUndoAnnotation = viewModel::undoAnnotation,
                onRedoAnnotation = viewModel::redoAnnotation,
                onDeleteSelectedAnnotation = viewModel::deleteSelectedAnnotation,
                onClearCurrentFrameAnnotations = viewModel::clearCurrentFrameAnnotations,
                onDismissError = viewModel::dismissError,
            )
        }
    }

    state.relinkConflict?.let { conflict ->
        RelinkConflictDialog(
            conflict = conflict,
            onConfirm = viewModel::confirmRelinkConflict,
            onCancel = viewModel::cancelRelinkConflict,
        )
    }
}
