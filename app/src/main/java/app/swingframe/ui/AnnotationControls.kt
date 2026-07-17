package app.swingframe.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.swingframe.annotation.AnnotationTool
import app.swingframe.ui.theme.SwingFrameColors

private val AnnotationPalette = listOf(
    0xFFE5A63B.toInt(),
    0xFFF1F0E9.toInt(),
    0xFFFF6B62.toInt(),
    0xFF47C9FF.toInt(),
    0xFF71D18C.toInt(),
    0xFF8A72FF.toInt(),
)

private val AnnotationStrokeWidths = listOf(2f, 3f, 5f, 8f)

@Composable
fun AnnotationToolBar(
    selectedTool: AnnotationTool,
    enabled: Boolean,
    onSelectTool: (AnnotationTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools = AnnotationTool.entries
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SwingFrameColors.Panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.StrokeSoft),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(tools, key = { it.name }) { tool ->
                val selected = tool == selectedTool
                Surface(
                    onClick = { onSelectTool(tool) },
                    enabled = enabled,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = tool.editorLabel
                            role = Role.Button
                            this.selected = selected
                        },
                    shape = RoundedCornerShape(3.dp),
                    color = if (selected) SwingFrameColors.Accent else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ToolGlyph(
                            tool = tool,
                            color = if (selected) SwingFrameColors.OnAccent else SwingFrameColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotationActionBar(
    tool: AnnotationTool,
    enabled: Boolean,
    colorArgb: Int,
    strokeWidthDp: Float,
    overlayVisible: Boolean,
    carryForward: Boolean,
    hasSelection: Boolean,
    hasAnnotations: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onSetColor: (Int) -> Unit,
    onSetStrokeWidth: (Float) -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleCarryForward: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextColor = AnnotationPalette[(AnnotationPalette.indexOf(colorArgb).takeIf { it >= 0 } ?: 0)
        .let { (it + 1) % AnnotationPalette.size }]
    val nextStroke = AnnotationStrokeWidths[(AnnotationStrokeWidths.indexOf(strokeWidthDp).takeIf { it >= 0 } ?: 0)
        .let { (it + 1) % AnnotationStrokeWidths.size }]

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = SwingFrameColors.Panel.copy(alpha = 0.97f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SwingFrameColors.Stroke),
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(horizontal = 7.dp)) {
                Text("TOOL", style = MaterialTheme.typography.labelSmall, color = SwingFrameColors.TextMuted)
                Text(tool.editorLabel, style = MaterialTheme.typography.labelMedium, color = SwingFrameColors.TextPrimary)
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(SwingFrameColors.Stroke),
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    CompactAction(
                        label = "Color",
                        enabled = enabled,
                        onClick = { onSetColor(nextColor) },
                    ) {
                        Box(
                            Modifier
                                .size(15.dp)
                                .background(Color(colorArgb), CircleShape)
                                .border(1.dp, SwingFrameColors.TextPrimary.copy(alpha = 0.45f), CircleShape),
                        )
                    }
                }
                item {
                    CompactAction(
                        label = "${strokeWidthDp.toInt()} pt",
                        enabled = enabled,
                        onClick = { onSetStrokeWidth(nextStroke) },
                    ) {
                        Canvas(Modifier.size(17.dp)) {
                            drawLine(
                                color = SwingFrameColors.TextPrimary,
                                start = Offset(1.dp.toPx(), center.y),
                                end = Offset(size.width - 1.dp.toPx(), center.y),
                                strokeWidth = strokeWidthDp.dp.toPx().coerceAtMost(7.dp.toPx()),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
                item {
                    CompactAction(
                        label = if (overlayVisible) "Hide" else "Show",
                        enabled = enabled,
                        onClick = onToggleOverlay,
                    ) {
                        Icon(
                            if (overlayVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = SwingFrameColors.TextPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                item {
                    CompactAction(
                        label = "Carry",
                        enabled = enabled,
                        active = carryForward,
                        onClick = onToggleCarryForward,
                    ) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = null,
                            tint = if (carryForward) SwingFrameColors.OnAccent else SwingFrameColors.TextPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                item {
                    CompactAction(label = "Undo", enabled = enabled && canUndo, onClick = onUndo) {
                        Icon(Icons.Filled.Undo, null, modifier = Modifier.size(17.dp))
                    }
                }
                item {
                    CompactAction(label = "Redo", enabled = enabled && canRedo, onClick = onRedo) {
                        Icon(Icons.Filled.Redo, null, modifier = Modifier.size(17.dp))
                    }
                }
                item {
                    CompactAction(label = "Delete", enabled = enabled && hasSelection, onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(17.dp))
                    }
                }
                item {
                    CompactAction(label = "Clear", enabled = enabled && hasAnnotations, onClick = onClear) {
                        Icon(Icons.Filled.LayersClear, null, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAction(
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(3.dp),
        color = if (active) SwingFrameColors.Accent else Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(Modifier.height(1.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    !enabled -> SwingFrameColors.TextMuted.copy(alpha = 0.35f)
                    active -> SwingFrameColors.OnAccent
                    else -> SwingFrameColors.TextMuted
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ToolGlyph(tool: AnnotationTool, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        when (tool) {
            AnnotationTool.SELECT -> {
                val path = Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.13f)
                    lineTo(size.width * 0.78f, size.height * 0.57f)
                    lineTo(size.width * 0.51f, size.height * 0.61f)
                    lineTo(size.width * 0.63f, size.height * 0.88f)
                    lineTo(size.width * 0.50f, size.height * 0.94f)
                    lineTo(size.width * 0.38f, size.height * 0.67f)
                    lineTo(size.width * 0.20f, size.height * 0.86f)
                    close()
                }
                drawPath(path, color)
            }
            AnnotationTool.LINE -> drawLine(
                color,
                Offset(size.width * 0.18f, size.height * 0.82f),
                Offset(size.width * 0.82f, size.height * 0.18f),
                stroke,
                StrokeCap.Round,
            )
            AnnotationTool.ANGLE -> {
                drawLine(color, Offset(size.width * 0.50f, size.height * 0.78f), Offset(size.width * 0.17f, size.height * 0.25f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.50f, size.height * 0.78f), Offset(size.width * 0.85f, size.height * 0.42f), stroke, StrokeCap.Round)
            }
            AnnotationTool.PLUMB_VERTICAL -> {
                drawLine(color, Offset(center.x, size.height * 0.12f), Offset(center.x, size.height * 0.88f), stroke, StrokeCap.Round)
                drawCircle(color, radius = 2.4.dp.toPx(), center = Offset(center.x, size.height * 0.17f), style = Stroke(stroke))
            }
            AnnotationTool.PLUMB_HORIZONTAL -> {
                drawLine(color, Offset(size.width * 0.12f, center.y), Offset(size.width * 0.88f, center.y), stroke, StrokeCap.Round)
                drawCircle(color, radius = 2.4.dp.toPx(), center = Offset(size.width * 0.17f, center.y), style = Stroke(stroke))
            }
            AnnotationTool.BOX -> drawRect(
                color,
                topLeft = Offset(size.width * 0.18f, size.height * 0.20f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.60f),
                style = Stroke(stroke),
            )
            AnnotationTool.CIRCLE -> drawCircle(color, radius = size.minDimension * 0.31f, style = Stroke(stroke))
            AnnotationTool.FREEHAND -> {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.70f)
                    cubicTo(size.width * 0.32f, size.height * 0.20f, size.width * 0.55f, size.height * 0.92f, size.width * 0.86f, size.height * 0.30f)
                }
                drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

private val AnnotationTool.editorLabel: String
    get() = when (this) {
        AnnotationTool.SELECT -> "Select"
        AnnotationTool.LINE -> "Line"
        AnnotationTool.ANGLE -> "Angle"
        AnnotationTool.PLUMB_VERTICAL -> "Plumb V"
        AnnotationTool.PLUMB_HORIZONTAL -> "Plumb H"
        AnnotationTool.BOX -> "Box"
        AnnotationTool.CIRCLE -> "Circle"
        AnnotationTool.FREEHAND -> "Freehand"
    }
