package app.swingframe.presentation.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import app.swingframe.presentation.ai.DiagnosticPanel
import app.swingframe.presentation.ai.SkeletonOverlay
import app.swingframe.ui.theme.BackgroundDark
import app.swingframe.ui.theme.LightGray
import app.swingframe.ui.theme.MoonstoneBlue
import app.swingframe.ui.theme.MustardGreen
import app.swingframe.ui.theme.SurfaceDark
import app.swingframe.ui.util.formatTime

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
        PreProcessingScreen(progress = state.preProcessProgress)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
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

                // Phase 2: AI Skeleton Layer (cached timeline, O(log n) nearest-frame lookup)
                if (state.isAiEnabled) {
                    val currentSkeleton = state.skeletonTimeline.skeletonAt(state.currentPosition)
                    SkeletonOverlay(
                        skeleton = currentSkeleton,
                        videoWidth = state.videoWidth,
                        videoHeight = state.videoHeight
                    )
                }
            }
        }

        // Surface playback/processing failures instead of a silent black screen
        state.errorMessage?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = { viewModel.onIntent(VideoPlayerIntent.ClearError) }
            )
        }

        // Custom Controller Area
        if (state.videoUri != null) {
            val report = state.aiReport

            // Phase 2: Show Diagnostic Panel Overlay if open
            if (state.isDiagnosticPanelOpen && report != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    DiagnosticPanel(
                        report = report,
                        onClose = { viewModel.onIntent(VideoPlayerIntent.ToggleDiagnosticPanel) }
                    )
                }
            }

            CustomVideoController(
                state = state,
                onIntent = { viewModel.onIntent(it) }
            )
        }
    }
}

@Composable
private fun PreProcessingScreen(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .safeDrawingPadding(),
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
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MustardGreen,
            trackColor = SurfaceDark
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            color = MustardGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "DISMISS",
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(start = 12.dp)
        )
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
