package app.swingframe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.swingframe.export.ExportStatus
import app.swingframe.export.ExportType
import app.swingframe.project.FrameBookmark
import app.swingframe.ui.theme.SwingFrameColors
import kotlin.math.roundToInt

@Composable
fun ExportSetupDialog(
    currentFrameIndex: Int,
    totalFrames: Int,
    bookmarks: List<FrameBookmark>,
    onDismiss: () -> Unit,
    onExportStill: (burnAnnotations: Boolean) -> Unit,
    onExportVideo: (startFrame: Int, endFrame: Int, speed: Float, burnAnnotations: Boolean) -> Unit,
) {
    var type by remember { mutableStateOf(ExportType.STILL) }
    var burnAnnotations by remember { mutableStateOf(true) }
    var startFrame by remember { mutableFloatStateOf(0f) }
    var endFrame by remember(totalFrames) { mutableFloatStateOf((totalFrames - 1).coerceAtLeast(0).toFloat()) }
    var speed by remember { mutableFloatStateOf(0.25f) }
    val lastFrame = (totalFrames - 1).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SwingFrameColors.Panel,
        title = { Text("Export analysis", color = SwingFrameColors.TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExportTypeButton("Still frame", type == ExportType.STILL) { type = ExportType.STILL }
                    ExportTypeButton("Video range", type == ExportType.VIDEO) { type = ExportType.VIDEO }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Burn annotations", color = SwingFrameColors.TextPrimary)
                        Text(
                            if (burnAnnotations) "Vectors are rendered into the output" else "Clean source pixels",
                            style = MaterialTheme.typography.labelMedium,
                            color = SwingFrameColors.TextMuted,
                        )
                    }
                    Switch(checked = burnAnnotations, onCheckedChange = { burnAnnotations = it })
                }

                if (type == ExportType.STILL) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = SwingFrameColors.PanelSoft,
                    ) {
                        Text(
                            "CURRENT FRAME  ${currentFrameIndex + 1}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = SwingFrameColors.Accent,
                        )
                    }
                } else {
                    if (bookmarks.size >= 2) {
                        Surface(
                            onClick = {
                                startFrame = bookmarks.minOf { it.frameIndex }.toFloat()
                                endFrame = bookmarks.maxOf { it.frameIndex }.toFloat()
                            },
                            shape = RoundedCornerShape(3.dp),
                            color = SwingFrameColors.PanelSoft,
                        ) {
                            Text(
                                "USE OUTERMOST MARKS",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = SwingFrameColors.Accent,
                            )
                        }
                    }
                    Text(
                        "RANGE  ${startFrame.roundToInt() + 1} — ${endFrame.roundToInt() + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SwingFrameColors.Accent,
                    )
                    Text("Start frame", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
                    Slider(
                        value = startFrame,
                        onValueChange = { startFrame = minOf(it, endFrame).coerceAtMost(lastFrame.toFloat()) },
                        valueRange = 0f..lastFrame.toFloat().coerceAtLeast(1f),
                    )
                    Text("End frame", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
                    Slider(
                        value = endFrame,
                        onValueChange = { endFrame = maxOf(it, startFrame).coerceAtMost(lastFrame.toFloat()) },
                        valueRange = 0f..lastFrame.toFloat().coerceAtLeast(1f),
                    )
                    Text("OUTPUT SPEED", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(0.25f, 0.5f, 1f).forEach { option ->
                            ExportTypeButton(
                                label = if (option == 1f) "1×" else "${option}×",
                                selected = speed == option,
                                onClick = { speed = option },
                            )
                        }
                    }
                    if (speed != 1f) {
                        Text(
                            "Slow-motion exports are silent to guarantee video timing stays correct.",
                            style = MaterialTheme.typography.labelMedium,
                            color = SwingFrameColors.TextMuted,
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SwingFrameColors.TextSecondary) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (type == ExportType.STILL) {
                        onExportStill(burnAnnotations)
                    } else {
                        onExportVideo(
                            startFrame.roundToInt().coerceIn(0, lastFrame),
                            endFrame.roundToInt().coerceIn(0, lastFrame),
                            speed,
                            burnAnnotations,
                        )
                    }
                    onDismiss()
                },
            ) {
                Text("Export", color = SwingFrameColors.Accent)
            }
        },
    )
}

@Composable
private fun ExportTypeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(3.dp),
        color = if (selected) SwingFrameColors.Accent else SwingFrameColors.PanelSoft,
    ) {
        Text(
            label.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SwingFrameColors.OnAccent else SwingFrameColors.TextSecondary,
        )
    }
}

@Composable
fun ExportStatusDialog(
    status: ExportStatus,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!status.isExporting && status.savedUri == null && status.errorMessage == null && status.message.isBlank()) return

    AlertDialog(
        onDismissRequest = { if (!status.isExporting) onDismiss() },
        containerColor = SwingFrameColors.Panel,
        title = {
            Text(
                when {
                    status.isExporting -> "Exporting locally"
                    status.errorMessage != null -> "Export failed"
                    status.savedUri != null -> "Export complete"
                    else -> "Export"
                },
                color = SwingFrameColors.TextPrimary,
            )
        },
        text = {
            Column {
                Text(
                    status.errorMessage ?: status.message,
                    color = if (status.errorMessage != null) SwingFrameColors.Error else SwingFrameColors.TextSecondary,
                )
                if (status.isExporting) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { status.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = SwingFrameColors.Accent,
                        trackColor = SwingFrameColors.PanelSoft,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "${status.progressPercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = SwingFrameColors.Accent,
                    )
                }
            }
        },
        confirmButton = {
            if (status.isExporting) {
                TextButton(onClick = onCancel) { Text("Cancel", color = SwingFrameColors.Error) }
            } else {
                TextButton(onClick = onDismiss) { Text("Done", color = SwingFrameColors.Accent) }
            }
        },
    )
}
