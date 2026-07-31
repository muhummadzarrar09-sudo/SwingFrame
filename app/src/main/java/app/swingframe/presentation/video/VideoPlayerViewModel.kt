package app.swingframe.presentation.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import app.swingframe.domain.ai.OnDevicePoseAnalyzer
import app.swingframe.domain.ai.VideoPreProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state.asStateFlow()

    private val poseAnalyzer = OnDevicePoseAnalyzer()
    private val preProcessor = VideoPreProcessor(application, poseAnalyzer)

    // The Media3 Player instance
    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        // Crucial for ultra-precise scrubbing: exact seeking rather than nearest keyframe
        setSeekParameters(SeekParameters.EXACT)
        
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { it.copy(duration = duration.coerceAtLeast(0)) }
                }
            }
        })
    }

    init {
        // Coroutine to poll playback progress
        viewModelScope.launch {
            while (true) {
                if (_state.value.isPlaying && !_state.value.isScrubbing) {
                    _state.update { 
                        it.copy(
                            currentPosition = player.currentPosition,
                            duration = player.duration.coerceAtLeast(0)
                        ) 
                    }
                }
                delay(16L) // ~60fps refresh for UI smoothness
            }
        }
    }

    fun onIntent(intent: VideoPlayerIntent) {
        when (intent) {
            is VideoPlayerIntent.SelectVideo -> {
                // Enter Pre-Processing Splash Screen
                _state.update { 
                    it.copy(
                        videoUri = intent.uri,
                        isPreProcessing = true,
                        preProcessProgress = 0f,
                        cachedSkeletons = emptyMap(),
                        isAiEnabled = true // Auto-enable AI since we're processing it
                    ) 
                }
                
                player.pause()
                
                // Launch the heavy background worker
                viewModelScope.launch {
                    val (skeletons, report) = preProcessor.processVideo(intent.uri) { progress ->
                        _state.update { it.copy(preProcessProgress = progress) }
                    }
                    
                    // Processing Complete -> Switch to Video Player Screen
                    withContext(Dispatchers.Main) {
                        _state.update { 
                            it.copy(
                                isPreProcessing = false,
                                cachedSkeletons = skeletons,
                                aiReport = report,
                                isDiagnosticPanelOpen = true // Auto open the report
                            ) 
                        }
                        player.setMediaItem(MediaItem.fromUri(intent.uri))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            }
            VideoPlayerIntent.TogglePlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
            is VideoPlayerIntent.SeekTo -> {
                player.seekTo(intent.position)
                _state.update { it.copy(currentPosition = intent.position) }
            }
            VideoPlayerIntent.ScrubStart -> {
                _state.update { it.copy(isScrubbing = true) }
                player.pause() // Auto-pause when user starts scrubbing
            }
            VideoPlayerIntent.ScrubEnd -> {
                _state.update { it.copy(isScrubbing = false) }
                // We keep it paused after scrubbing for precision review
            }
            is VideoPlayerIntent.SetPlaybackSpeed -> {
                player.playbackParameters = PlaybackParameters(intent.speed)
                _state.update { it.copy(playbackSpeed = intent.speed) }
            }
            VideoPlayerIntent.NextFrame -> {
                player.pause()
                val nextPos = (player.currentPosition + 33).coerceAtMost(player.duration)
                player.seekTo(nextPos)
                _state.update { it.copy(currentPosition = nextPos) }
            }
            VideoPlayerIntent.PreviousFrame -> {
                player.pause()
                val prevPos = (player.currentPosition - 33).coerceAtLeast(0)
                player.seekTo(prevPos)
                _state.update { it.copy(currentPosition = prevPos) }
            }
            VideoPlayerIntent.ToggleAi -> {
                val newState = !_state.value.isAiEnabled
                _state.update { it.copy(isAiEnabled = newState) }
            }
            VideoPlayerIntent.ToggleDiagnosticPanel -> {
                _state.update { it.copy(isDiagnosticPanelOpen = !it.isDiagnosticPanelOpen) }
            }
            is VideoPlayerIntent.UpdateProgress -> {
                _state.update { it.copy(currentPosition = intent.position, duration = intent.duration) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
