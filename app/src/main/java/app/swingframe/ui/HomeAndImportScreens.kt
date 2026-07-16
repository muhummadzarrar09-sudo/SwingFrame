package app.swingframe.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.swingframe.model.VideoSource
import app.swingframe.project.LocalProject
import app.swingframe.ui.theme.SwingFrameColors
import app.swingframe.util.formatBitrate
import app.swingframe.util.formatDurationUs
import app.swingframe.util.formatFps
import app.swingframe.util.formatResolution
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    recentProjects: List<LocalProject>,
    errorMessage: String?,
    onImport: () -> Unit,
    onOpenProject: (LocalProject) -> Unit,
    onRelinkProject: (LocalProject) -> Unit,
    onDeleteProject: (LocalProject) -> Unit,
    onDismissError: () -> Unit,
) {
    var pendingDeleteProject by remember { mutableStateOf<LocalProject?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwingFrameColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        EditorialWordmark(section = "LOCAL ANALYSIS / 001")
        Spacer(Modifier.height(72.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp, end = 16.dp)
                    .size(width = 3.dp, height = 82.dp)
                    .background(SwingFrameColors.Accent, CircleShape),
            )
            Text(
                text = "Your swing,\nheld to the frame.",
                style = MaterialTheme.typography.displaySmall,
                color = SwingFrameColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "A private workspace for exact timing, deliberate geometry, and repeatable analysis.",
            style = MaterialTheme.typography.bodyLarge,
            color = SwingFrameColors.TextSecondary,
            modifier = Modifier.fillMaxWidth(0.92f),
        )

        Spacer(Modifier.height(34.dp))
        Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SwingFrameColors.Accent,
                contentColor = SwingFrameColors.OnAccent,
            ),
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text("Open a swing", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(14.dp))
        AnimatedVisibility(visible = errorMessage != null) {
            if (errorMessage != null) ErrorCard(errorMessage, onDismissError)
        }

        if (recentProjects.isEmpty()) {
            Spacer(Modifier.weight(1f))
            AnalysisPrinciples()
        } else {
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("RECENT PROJECTS", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
                Text("${recentProjects.size} LOCAL", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.Accent)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(recentProjects, key = { it.id }) { project ->
                    RecentProjectRow(
                        project = project,
                        onOpen = { onOpenProject(project) },
                        onRelink = { onRelinkProject(project) },
                        onDelete = { pendingDeleteProject = project },
                    )
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    AnalysisPrinciples()
                }
            }
        }
        Spacer(Modifier.height(22.dp))
    }

    pendingDeleteProject?.let { project ->
        DeleteProjectDialog(
            project = project,
            onConfirm = {
                onDeleteProject(project)
                pendingDeleteProject = null
            },
            onCancel = { pendingDeleteProject = null },
        )
    }
}

@Composable
private fun RecentProjectRow(
    project: LocalProject,
    onOpen: () -> Unit,
    onRelink: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = rememberProjectDate(project.updatedAtEpochMs)
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = SwingFrameColors.Panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.StrokeSoft),
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 42.dp)
                    .background(SwingFrameColors.Accent, CircleShape),
            )
            Spacer(Modifier.size(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = SwingFrameColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "$date  /  ${project.totalFrames} FRAMES  /  LAST ${project.lastFrameIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwingFrameColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.bookmarks.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${project.bookmarks.size} MARK${if (project.bookmarks.size == 1) "" else "S"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SwingFrameColors.Accent,
                    )
                }
            }
            IconButton(onClick = onRelink, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Link, "Relink source", tint = SwingFrameColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.DeleteOutline, "Delete local project", tint = SwingFrameColors.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun rememberProjectDate(epochMs: Long): String {
    return androidx.compose.runtime.remember(epochMs) {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochMs)).uppercase(Locale.getDefault())
    }
}

@Composable
private fun EditorialWordmark(section: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "SWINGFRAME",
            style = MaterialTheme.typography.labelLarge,
            color = SwingFrameColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            section,
            style = MaterialTheme.typography.labelSmall,
            color = SwingFrameColors.Accent,
        )
    }
}

@Composable
private fun AnalysisPrinciples() {
    Column {
        HorizontalDivider(color = SwingFrameColors.Stroke)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Principle("01", "Exact timing")
            Principle("02", "On device")
            Principle("03", "No account")
        }
        HorizontalDivider(color = SwingFrameColors.Stroke)
        Spacer(Modifier.height(15.dp))
        Text(
            "FIELD NOTE",
            style = MaterialTheme.typography.labelSmall,
            color = SwingFrameColors.TextMuted,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Import the original clip. SwingFrame indexes its real presentation timestamps before analysis.",
            style = MaterialTheme.typography.bodyMedium,
            color = SwingFrameColors.TextSecondary,
        )
    }
}

@Composable
private fun Principle(number: String, label: String) {
    Column {
        Text(number, style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.Accent)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = SwingFrameColors.TextPrimary)
    }
}

@Composable
fun ImportPreviewScreen(
    source: VideoSource,
    errorMessage: String?,
    onAnalyze: () -> Unit,
    onChooseAnother: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val metadata = source.metadata
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwingFrameColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = SwingFrameColors.TextPrimary)
            }
            Text(
                "SOURCE REVIEW",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = SwingFrameColors.Accent,
            )
            Text("01 / 02", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
            Spacer(Modifier.size(12.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                source.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = SwingFrameColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .border(1.dp, SwingFrameColors.Stroke, RoundedCornerShape(6.dp)),
                shape = RoundedCornerShape(6.dp),
                color = SwingFrameColors.Panel,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawLine(
                            color = SwingFrameColors.Accent.copy(alpha = 0.82f),
                            start = Offset(size.width * 0.16f, size.height * 0.83f),
                            end = Offset(size.width * 0.74f, size.height * 0.18f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = SwingFrameColors.Stroke,
                            start = Offset(size.width * 0.08f, size.height * 0.22f),
                            end = Offset(size.width * 0.92f, size.height * 0.22f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    Text(
                        "SOURCE / READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = SwingFrameColors.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(25.dp))
            Text("TECHNICAL INDEX", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
            Spacer(Modifier.height(8.dp))
            MetadataLedger(
                resolution = formatResolution(metadata.width, metadata.height, metadata.rotationDegrees),
                duration = formatDurationUs(metadata.durationUs),
                fps = metadata.declaredFps?.let(::formatFps) ?: "Unknown",
                codec = metadata.codecLabel,
                frameCount = metadata.estimatedFrameCount?.let { "Approx. $it" } ?: "Read on scan",
                bitrate = formatBitrate(metadata.bitrate),
            )

            Spacer(Modifier.height(14.dp))
            AnimatedVisibility(visible = errorMessage != null) {
                if (errorMessage != null) ErrorCard(errorMessage, onDismissError)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Button(
                onClick = onAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SwingFrameColors.Accent,
                    contentColor = SwingFrameColors.OnAccent,
                ),
            ) {
                Text("Build exact timeline", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onChooseAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(47.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = SwingFrameColors.TextSecondary,
                ),
            ) {
                Text("Choose another source")
            }
        }
    }
}

@Composable
private fun MetadataLedger(
    resolution: String,
    duration: String,
    fps: String,
    codec: String,
    frameCount: String,
    bitrate: String,
) {
    Column {
        LedgerRow("Resolution", resolution, "Duration", duration)
        LedgerRow("Timing", fps, "Codec", codec)
        LedgerRow("Frame estimate", frameCount, "Data rate", bitrate)
    }
}

@Composable
private fun LedgerRow(labelA: String, valueA: String, labelB: String, valueB: String) {
    HorizontalDivider(color = SwingFrameColors.StrokeSoft)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LedgerCell(labelA, valueA, Modifier.weight(1f))
        LedgerCell(labelB, valueB, Modifier.weight(1f))
    }
}

@Composable
private fun LedgerCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = SwingFrameColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LoadingScreen(
    label: String,
    progress: Float?,
    detail: String? = null,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwingFrameColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            EditorialWordmark(section = "PROCESS / 02")
            Spacer(Modifier.weight(0.62f))
            Text(
                if (progress == null) "—" else "${(progress * 100).toInt().coerceIn(0, 100)}",
                style = MaterialTheme.typography.displaySmall,
                color = SwingFrameColors.Accent,
            )
            Text(
                if (progress == null) "PREPARING" else "PERCENT",
                style = MaterialTheme.typography.labelSmall,
                color = SwingFrameColors.TextMuted,
            )
            Spacer(Modifier.height(20.dp))
            Text(label, style = MaterialTheme.typography.headlineMedium, color = SwingFrameColors.TextPrimary)
            if (detail != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    detail,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SwingFrameColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(22.dp))
            if (progress == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = SwingFrameColors.Accent,
                    strokeWidth = 2.dp,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = SwingFrameColors.Accent,
                    trackColor = SwingFrameColors.PanelSoft,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Reading actual presentation timestamps. No assumed frame rate, no network request.",
                style = MaterialTheme.typography.bodyMedium,
                color = SwingFrameColors.TextMuted,
            )
            Spacer(Modifier.weight(1f))
        }

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 8.dp),
        ) {
            Icon(Icons.Filled.Close, "Cancel", tint = SwingFrameColors.TextSecondary)
        }
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SwingFrameColors.Error.copy(alpha = 0.45f), RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        color = SwingFrameColors.Error.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = SwingFrameColors.TextPrimary,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Close, "Dismiss", tint = SwingFrameColors.TextSecondary)
            }
        }
    }
}
