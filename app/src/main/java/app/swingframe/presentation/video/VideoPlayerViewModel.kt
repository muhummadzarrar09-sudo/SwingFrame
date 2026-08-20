package app.swingframe.presentation.video

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import app.swingframe.domain.ai.AnalysisQuality
import app.swingframe.domain.ai.OnDevicePoseAnalyzer
import app.swingframe.domain.ai.SkeletonTimeline
import app.swingframe.domain.ai.SwingAnalysisResult
import app.swingframe.domain.ai.VideoPreProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state.asStateFlow()

    private val poseAnalyzer = OnDevicePoseAnalyzer()
    private val preProcessor = VideoPreProcessor(application, poseAnalyzer)

    private var processingJob: Job? = null
    private var progressTicker: Job? = null

    // The Media3 Player instance
    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        // Crucial for ultra-precise scrubbing: exact seeking rather than nearest keyframe
        setSeekParameters(SeekParameters.EXACT)

        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTicker() else stopProgressTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { it.copy(duration = duration.coerceAtLeast(0)) }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _state.update {
                    it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error", error)
                _state.update {
                    it.copy(
                        errorMessage = "Playback failed (${error.errorCodeName}). The video may be corrupt or unsupported.",
                        isPlaying = false
                    )
                }
                stopProgressTicker()
            }
        })
    }

    fun onIntent(intent: VideoPlayerIntent) {
        when (intent) {
            is VideoPlayerIntent.SelectVideo -> {
                // Cancel any in-flight analysis of a previously selected video.
                processingJob?.cancel()

                _state.update {
                    it.copy(
                        videoUri = intent.uri,
                        isPreProcessing = true,
                        preProcessProgress = 0f,
                        skeletonTimeline = SkeletonTimeline.EMPTY,
                        aiReport = null,
                        isDiagnosticPanelOpen = false,
                        errorMessage = null,
                        isAiEnabled = true // Auto-enable AI since we're processing it
                    )
                }

                player.pause()

                // viewModelScope launches on Main; VideoPreProcessor hops to Dispatchers.Default.
                processingJob = viewModelScope.launch {
                    val result = preProcessor.processVideo(intent.uri) { progress ->
                        _state.update { it.copy(preProcessProgress = progress) }
                    }

                    when (result) {
                        is SwingAnalysisResult.Success -> {
                            _state.update {
                                it.copy(
                                    isPreProcessing = false,
                                    skeletonTimeline = SkeletonTimeline.from(result.skeletons),
                                    aiReport = result.report,
                                    // Only auto-open the report when it actually contains a diagnosis
                                    isDiagnosticPanelOpen = result.report.quality == AnalysisQuality.RELIABLE
                                )
                            }
                            player.setMediaItem(MediaItem.fromUri(intent.uri))
                            player.prepare()
                            player.playWhenReady = true
                        }

                        is SwingAnalysisResult.Failure -> {
                            _state.update {
                                it.copy(
                                    isPreProcessing = false,
                                    isAiEnabled = false,
                                    errorMessage = result.message
                                )
                            }
                        }
                    }
                }
            }

            VideoPlayerIntent.TogglePlayPause -> {
                if (player.isPlaying) player.pause() else player.play()
            }

            is VideoPlayerIntent.SeekTo -> {
                val maxPosition = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                val clamped = intent.position.coerceIn(0L, maxPosition)
                player.seekTo(clamped)
                _state.update { it.copy(currentPosition = clamped) }
            }

            is VideoPlayerIntent.SetPlaybackSpeed -> {
                player.playbackParameters = PlaybackParameters(intent.speed)
                _state.update { it.copy(playbackSpeed = intent.speed) }
            }

            VideoPlayerIntent.NextFrame -> stepFrame(forward = true)
            VideoPlayerIntent.PreviousFrame -> stepFrame(forward = false)

            VideoPlayerIntent.ToggleAi -> {
                _state.update { it.copy(isAiEnabled = !it.isAiEnabled) }
            }

            VideoPlayerIntent.ToggleDiagnosticPanel -> {
                _state.update { it.copy(isDiagnosticPanelOpen = !it.isDiagnosticPanelOpen) }
            }

            VideoPlayerIntent.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    /**
     * Steps by one video frame. The step size is derived from the actual frame rate of the
     * loaded video (a fixed 33 ms is wrong for 24/48/60 fps footage), falling back to 30 fps.
     */
    private fun stepFrame(forward: Boolean) {
        player.pause()
        val stepMs = frameStepMs()
        val maxPosition = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = if (forward) {
            (player.currentPosition + stepMs).coerceAtMost(maxPosition)
        } else {
            (player.currentPosition - stepMs).coerceAtLeast(0L)
        }
        player.seekTo(target)
        _state.update { it.copy(currentPosition = player.currentPosition) }
    }

    private fun frameStepMs(): Long {
        val fps = player.videoFormat?.frameRate ?: DEFAULT_FPS
        return if (fps > 0f) {
            (1000.0 / fps).toLong().coerceAtLeast(1L)
        } else {
            DEFAULT_FRAME_STEP_MS
        }
    }

    /** Progress is only polled while playback is actually running; no busy loop when idle. */
    private fun startProgressTicker() {
        if (progressTicker?.isActive == true) return
        progressTicker = viewModelScope.launch {
            while (true) {
                _state.update {
                    it.copy(
                        currentPosition = player.currentPosition,
                        duration = player.duration.coerceAtLeast(0)
                    )
                }
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTicker?.cancel()
        progressTicker = null
        _state.update { it.copy(currentPosition = player.currentPosition) }
    }

    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
        stopProgressTicker()
        player.release()
        poseAnalyzer.close()
    }

    private companion object {
        const val TAG = "VideoPlayerViewModel"
        const val PROGRESS_TICK_MS = 100L
        const val DEFAULT_FPS = 30f
        const val DEFAULT_FRAME_STEP_MS = 33L
    }
}
