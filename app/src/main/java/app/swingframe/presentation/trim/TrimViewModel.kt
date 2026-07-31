package app.swingframe.presentation.trim

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrimViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TrimState())
    val state: StateFlow<TrimState> = _state.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        setSeekParameters(SeekParameters.EXACT)
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }
        })
    }

    init {
        // Enforce trim boundaries during playback
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    val endPos = _state.value.endTrimPosition
                    
                    if (endPos > 0 && currentPos >= endPos) {
                        player.pause()
                        player.seekTo(_state.value.startTrimPosition)
                        _state.update { it.copy(currentPosition = _state.value.startTrimPosition) }
                    } else {
                        _state.update { it.copy(currentPosition = currentPos) }
                    }
                }
                delay(16L)
            }
        }
    }

    fun onIntent(intent: TrimIntent) {
        when (intent) {
            is TrimIntent.LoadVideo -> {
                _state.update { 
                    it.copy(
                        videoUri = intent.uri, 
                        duration = intent.duration,
                        endTrimPosition = intent.duration // Default end trim to full duration
                    ) 
                }
                player.setMediaItem(MediaItem.fromUri(intent.uri))
                player.prepare()
            }
            is TrimIntent.UpdateStartTrim -> {
                _state.update { it.copy(startTrimPosition = intent.position) }
                player.seekTo(intent.position)
            }
            is TrimIntent.UpdateEndTrim -> {
                _state.update { it.copy(endTrimPosition = intent.position) }
                player.seekTo(intent.position)
            }
            is TrimIntent.SeekTo -> {
                player.seekTo(intent.position)
                _state.update { it.copy(currentPosition = intent.position) }
            }
            TrimIntent.TogglePlayPause -> {
                if (player.isPlaying) player.pause() else {
                    // If we are at the end boundary, loop back to start boundary before playing
                    if (player.currentPosition >= _state.value.endTrimPosition) {
                        player.seekTo(_state.value.startTrimPosition)
                    }
                    player.play()
                }
            }
            TrimIntent.StartExport -> {
                // TODO: Wire up actual FFmpeg or Media3 Transformer export
                // For now, mock the export progress for UI
                _state.update { it.copy(isExporting = true, exportProgress = 0f) }
                viewModelScope.launch {
                    for (i in 1..100) {
                        delay(20) // Mocking time
                        _state.update { it.copy(exportProgress = i / 100f) }
                    }
                    _state.update { it.copy(isExporting = false) }
                }
            }
            is TrimIntent.UpdateProgress -> {
                _state.update { it.copy(currentPosition = intent.position) }
            }
            is TrimIntent.UpdateExportProgress -> {
                _state.update { it.copy(exportProgress = intent.progress) }
            }
            TrimIntent.ExportComplete -> {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
