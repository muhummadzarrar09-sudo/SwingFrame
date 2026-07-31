package app.swingframe.presentation.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import app.swingframe.ui.theme.*

import app.swingframe.presentation.ai.SkeletonOverlay

import kotlin.math.abs

import app.swingframe.presentation.ai.DiagnosticPanel
import app.swingframe.domain.ai.SwingReport

@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onIntent(VideoPlayerIntent.SelectVideo(it)) }
    }

    if (state.isPreProcessing) {
        // --- THE AI SPLASH LOADING SCREEN ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "INITIALIZING AI ENGINE",
                color = MoonstoneBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ANALYZING SWING BIOMECHANICS...",
                color = LightGray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { state.preProcessProgress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MustardGreen,
                trackColor = SurfaceDark
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${(state.preProcessProgress * 100).toInt()}%",
                color = MustardGreen,
                fontWeight = FontWeight.Bold
            )
        }
        return // Do not render the video player behind the splash screen
    }

    // --- THE STANDARD VIDEO PLAYER UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SWING",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import", tint = BackgroundDark)
                Spacer(Modifier.width(8.dp))
                Text("IMPORT", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        }

        // Video Player View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (state.videoUri == null) {
                Text(
                    text = "NO VIDEO LOADED",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center),
                    letterSpacing = 2.sp
                )
            } else {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = viewModel.player
                            useController = false // We are building a custom controller
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Phase 2: AI Skeleton Layer (Pulled from Cache!)
                if (state.isAiEnabled) {
                    val currentSkeleton = state.cachedSkeletons.entries
                        .minByOrNull { abs(it.key - state.currentPosition) }?.value
                        
                    SkeletonOverlay(skeleton = currentSkeleton)
                }
            }
        }

        // Custom Controller Area
        if (state.videoUri != null) {
            
            // Phase 2: Show Diagnostic Panel Overlay if open
            if (state.isDiagnosticPanelOpen && state.aiReport != null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    DiagnosticPanel(
                        report = state.aiReport!!,
                        onClose = { viewModel.onIntent(VideoPlayerIntent.ToggleDiagnosticPanel) }
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(0.01f)) // Just to keep layout balanced when closed
            }

            // Temporary Trim UI integration for MVP testing
            var startTrim by remember { mutableStateOf(0L) }
            var endTrim by remember { mutableStateOf(state.duration) }
            
            // Sync end trim when duration loads
            LaunchedEffect(state.duration) {
                if (endTrim == 0L && state.duration > 0L) {
                    endTrim = state.duration
                }
            }

            app.swingframe.presentation.trim.RangeSliderTrimmer(
                duration = state.duration,
                startPosition = startTrim,
                endPosition = endTrim,
                onStartChange = { startTrim = it },
                onEndChange = { endTrim = it }
            )

            CustomVideoController(
                state = state,
                onIntent = { viewModel.onIntent(it) }
            )
        }
    }
}

@Composable
fun CustomVideoController(
    state: VideoPlayerState,
    onIntent: (VideoPlayerIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Scrubber / Timeline
        Slider(
            value = if (state.duration > 0) state.currentPosition.toFloat() / state.duration else 0f,
            onValueChange = { percent ->
                val newPosition = (percent * state.duration).toLong()
                onIntent(VideoPlayerIntent.SeekTo(newPosition))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        )

        // Time indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(state.currentPosition), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text(formatTime(state.duration), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Control
            Text(
                text = "${state.playbackSpeed}x",
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    val nextSpeed = when (state.playbackSpeed) {
                        1.0f -> 0.5f
                        0.5f -> 0.25f
                        0.25f -> 0.1f
                        else -> 1.0f
                    }
                    onIntent(VideoPlayerIntent.SetPlaybackSpeed(nextSpeed))
                }
            )

            // Frame Backward
            IconButton(onClick = { onIntent(VideoPlayerIntent.PreviousFrame) }) {
                Text("<|", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }

            // Play / Pause
            FloatingActionButton(
                onClick = { onIntent(VideoPlayerIntent.TogglePlayPause) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = BackgroundDark,
                modifier = Modifier.size(56.dp)
            ) {
                if (state.isPlaying) {
                    Text("||", fontWeight = FontWeight.Black, fontSize = 20.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
                }
            }

            // Frame Forward
            IconButton(onClick = { onIntent(VideoPlayerIntent.NextFrame) }) {
                Text("|>", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }

            // Draw Lines Toggle (Phase 2: AI Trace)
            Text(
                text = if (state.isAiEnabled) "AI TRACE: ON" else "AI TRACE: OFF",
                color = if (state.isAiEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onIntent(VideoPlayerIntent.ToggleAi) }
            )
            
            // Diagnostics Button
            if (state.aiReport != null) {
                Text(
                    text = "AI COACH",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onIntent(VideoPlayerIntent.ToggleDiagnosticPanel) }
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (ms % 1000) / 10 // showing two digits for ms
    return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
}
