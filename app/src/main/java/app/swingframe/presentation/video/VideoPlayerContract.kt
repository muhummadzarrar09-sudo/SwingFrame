package app.swingframe.presentation.video

import android.net.Uri

import app.swingframe.domain.ai.SwingSkeleton

import app.swingframe.domain.ai.SwingReport

data class VideoPlayerState(
    val videoUri: Uri? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isScrubbing: Boolean = false,
    
    // Phase 2: AI Pre-Processing State
    val isPreProcessing: Boolean = false,
    val preProcessProgress: Float = 0f,
    val isAiEnabled: Boolean = false,
    val cachedSkeletons: Map<Long, SwingSkeleton> = emptyMap(),
    val aiReport: SwingReport? = null,
    val isDiagnosticPanelOpen: Boolean = false
)

sealed interface VideoPlayerIntent {
    data class SelectVideo(val uri: Uri) : VideoPlayerIntent
    object TogglePlayPause : VideoPlayerIntent
    data class SeekTo(val position: Long) : VideoPlayerIntent
    object ScrubStart : VideoPlayerIntent
    object ScrubEnd : VideoPlayerIntent
    data class SetPlaybackSpeed(val speed: Float) : VideoPlayerIntent
    object NextFrame : VideoPlayerIntent
    object PreviousFrame : VideoPlayerIntent
    
    // Phase 2: AI Intents
    object ToggleAi : VideoPlayerIntent
    object ToggleDiagnosticPanel : VideoPlayerIntent
    
    // Internal intent for exo player to update state back to VM
    data class UpdateProgress(val position: Long, val duration: Long) : VideoPlayerIntent
}
