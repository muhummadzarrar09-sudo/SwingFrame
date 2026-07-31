package app.swingframe.presentation.trim

import android.net.Uri

data class TrimState(
    val videoUri: Uri? = null,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val startTrimPosition: Long = 0L,
    val endTrimPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f
)

sealed interface TrimIntent {
    data class LoadVideo(val uri: Uri, val duration: Long) : TrimIntent
    data class UpdateStartTrim(val position: Long) : TrimIntent
    data class UpdateEndTrim(val position: Long) : TrimIntent
    data class SeekTo(val position: Long) : TrimIntent
    object TogglePlayPause : TrimIntent
    object StartExport : TrimIntent
    
    // Internal
    data class UpdateProgress(val position: Long) : TrimIntent
    data class UpdateExportProgress(val progress: Float) : TrimIntent
    object ExportComplete : TrimIntent
}
