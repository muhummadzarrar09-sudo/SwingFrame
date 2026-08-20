package app.swingframe.presentation.video

import android.net.Uri
import app.swingframe.domain.ai.SkeletonTimeline
import app.swingframe.domain.ai.SwingReport

data class VideoPlayerState(
    val videoUri: Uri? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,

    // Phase 2: AI Pre-Processing State
    val isPreProcessing: Boolean = false,
    val preProcessProgress: Float = 0f,
    val isAiEnabled: Boolean = false,
    val skeletonTimeline: SkeletonTimeline = SkeletonTimeline.EMPTY,
    val aiReport: SwingReport? = null,
    val isDiagnosticPanelOpen: Boolean = false,

    // Video metadata used to align the skeleton overlay with the letterboxed video
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,

    // Non-null when playback/processing fails; surfaced to the user instead of a silent black screen
    val errorMessage: String? = null
)

sealed interface VideoPlayerIntent {
    data class SelectVideo(val uri: Uri) : VideoPlayerIntent
    object TogglePlayPause : VideoPlayerIntent
    data class SeekTo(val position: Long) : VideoPlayerIntent
    data class SetPlaybackSpeed(val speed: Float) : VideoPlayerIntent
    object NextFrame : VideoPlayerIntent
    object PreviousFrame : VideoPlayerIntent

    // Phase 2: AI Intents
    object ToggleAi : VideoPlayerIntent
    object ToggleDiagnosticPanel : VideoPlayerIntent
    object ClearError : VideoPlayerIntent
}
