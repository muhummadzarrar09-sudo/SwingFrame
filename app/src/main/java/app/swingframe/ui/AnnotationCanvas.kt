package app.swingframe.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.swingframe.annotation.AngleAnnotation
import app.swingframe.annotation.AnnotationShape
import app.swingframe.annotation.AnnotationStyle
import app.swingframe.annotation.AnnotationTool
import app.swingframe.annotation.BoxAnnotation
import app.swingframe.annotation.CircleAnnotation
import app.swingframe.annotation.FreehandAnnotation
import app.swingframe.annotation.LineAnnotation
import app.swingframe.annotation.NormalizedPoint
import app.swingframe.annotation.PlumbAnnotation
import app.swingframe.annotation.PlumbOrientation
import app.swingframe.annotation.angleDegrees
import app.swingframe.annotation.distanceTo
import app.swingframe.annotation.handles
import app.swingframe.annotation.translated
import app.swingframe.annotation.withHandle
import app.swingframe.ui.theme.SwingFrameColors
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class PendingAngle(
    val id: String,
    val vertex: NormalizedPoint,
    val armA: NormalizedPoint,
)

private sealed interface SelectDrag {
    val original: AnnotationShape
    data class Move(override val original: AnnotationShape, val down: NormalizedPoint) : SelectDrag
    data class Handle(override val original: AnnotationShape, val handleIndex: Int) : SelectDrag
}

@Composable
fun AnnotationCanvas(
    frameIndex: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    annotations: List<AnnotationShape>,
    carriedAnnotations: List<AnnotationShape>,
    selectedAnnotationId: String?,
    tool: AnnotationTool,
    style: AnnotationStyle,
    overlayVisible: Boolean,
    onSelect: (String?) -> Unit,
    onAdd: (AnnotationShape) -> Unit,
    onReplace: (AnnotationShape) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(frameIndex, tool) { mutableStateOf<AnnotationShape?>(null) }
    var pendingAngle by remember(frameIndex, tool) { mutableStateOf<PendingAngle?>(null) }
    val currentAnnotations by rememberUpdatedState(annotations)
    val currentSelectedId by rememberUpdatedState(selectedAnnotationId)
    val angleTextPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }

    if (!overlayVisible) return

    val gestureModifier = Modifier.pointerInput(
        frameIndex,
        tool,
        style,
        bitmapWidth,
        bitmapHeight,
    ) {
        awaitEachGesture {
            val downChange = awaitFirstDown(requireUnconsumed = false)
            val imageRect = fittedImageRect(
                container = Size(size.width.toFloat(), size.height.toFloat()),
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight,
            )
            if (!imageRect.contains(downChange.position)) return@awaitEachGesture

            val down = imageRect.toNormalized(downChange.position)
            var selectDrag: SelectDrag? = null
            var moved = false

            when (tool) {
                AnnotationTool.SELECT -> {
                    val selected = currentAnnotations.firstOrNull { it.id == currentSelectedId }
                    val handle = selected?.handles()?.minByOrNull { handle ->
                        imageRect.pixelDistance(handle.point, downChange.position)
                    }?.takeIf { handle ->
                        imageRect.pixelDistance(handle.point, downChange.position) <= HANDLE_HIT_RADIUS_DP.dp.toPx()
                    }

                    if (selected != null && handle != null) {
                        selectDrag = SelectDrag.Handle(selected, handle.index)
                        draft = selected
                    } else {
                        val hitRadiusPx = SHAPE_HIT_RADIUS_DP.dp.toPx()
                        val hit = currentAnnotations.asReversed().firstOrNull { shape ->
                            shape.distanceTo(down, imageRect.width, imageRect.height) <= hitRadiusPx
                        }
                        onSelect(hit?.id)
                        if (hit != null) {
                            selectDrag = SelectDrag.Move(hit, down)
                            draft = hit
                        } else {
                            draft = null
                        }
                    }
                }

                AnnotationTool.LINE -> {
                    draft = LineAnnotation(start = down, end = down, style = style)
                }

                AnnotationTool.ANGLE -> {
                    val pending = pendingAngle
                    draft = if (pending == null) {
                        AngleAnnotation(vertex = down, armA = down, armB = down, style = style)
                    } else {
                        AngleAnnotation(
                            id = pending.id,
                            vertex = pending.vertex,
                            armA = pending.armA,
                            armB = down,
                            style = style,
                        )
                    }
                }

                AnnotationTool.PLUMB_VERTICAL -> {
                    draft = PlumbAnnotation(
                        orientation = PlumbOrientation.VERTICAL,
                        position = down.x,
                        style = style,
                    )
                }

                AnnotationTool.PLUMB_HORIZONTAL -> {
                    draft = PlumbAnnotation(
                        orientation = PlumbOrientation.HORIZONTAL,
                        position = down.y,
                        style = style,
                    )
                }

                AnnotationTool.BOX -> {
                    draft = BoxAnnotation(cornerA = down, cornerB = down, style = style)
                }

                AnnotationTool.CIRCLE -> {
                    draft = CircleAnnotation(cornerA = down, cornerB = down, style = style)
                }

                AnnotationTool.FREEHAND -> {
                    draft = FreehandAnnotation(points = listOf(down), style = style)
                }
            }

            while (true) {
                val event = awaitPointerEvent()
                val pressedChanges = event.changes.filter { it.pressed }
                if (pressedChanges.size > 1) {
                    // Leave multi-touch unconsumed for parent zoom/pan and cancel this draft.
                    draft = null
                    return@awaitEachGesture
                }
                val change = event.changes.firstOrNull { it.id == downChange.id } ?: break
                if (!change.pressed) break
                if (!imageRect.contains(change.position)) continue

                val current = imageRect.toNormalized(change.position)
                moved = moved || imageRect.pixelDistance(down, change.position) > DRAG_SLOP_DP.dp.toPx()

                draft = when (val active = draft) {
                    is LineAnnotation -> active.copy(end = current)
                    is AngleAnnotation -> {
                        if (pendingAngle == null) active.copy(armA = current) else active.copy(armB = current)
                    }
                    is PlumbAnnotation -> active.copy(
                        position = if (active.orientation == PlumbOrientation.VERTICAL) current.x else current.y,
                    )
                    is BoxAnnotation -> active.copy(cornerB = current)
                    is CircleAnnotation -> active.copy(cornerB = current)
                    is FreehandAnnotation -> {
                        val previous = active.points.lastOrNull()
                        val farEnough = previous == null ||
                            imageRect.pixelDistance(previous, change.position) >= FREEHAND_STEP_DP.dp.toPx()
                        if (farEnough && active.points.size < MAX_FREEHAND_POINTS) {
                            active.copy(points = active.points + current)
                        } else {
                            active
                        }
                    }
                    else -> active
                }

                if (tool == AnnotationTool.SELECT && selectDrag != null) {
                    draft = when (val drag = selectDrag) {
                        is SelectDrag.Handle -> drag.original.withHandle(drag.handleIndex, current)
                        is SelectDrag.Move -> drag.original.translated(
                            dx = current.x - drag.down.x,
                            dy = current.y - drag.down.y,
                        )
                        null -> draft
                    }
                }
                change.consume()
            }

            val completed = draft
            when (tool) {
                AnnotationTool.SELECT -> {
                    if (moved && completed != null && selectDrag != null && completed != selectDrag.original) {
                        onReplace(completed)
                    }
                    draft = null
                }

                AnnotationTool.ANGLE -> {
                    val angle = completed as? AngleAnnotation
                    if (angle != null && pendingAngle == null && moved && imageRect.pixelDistance(angle.vertex, imageRect.toOffset(angle.armA)) >= MIN_SHAPE_SIZE_DP.dp.toPx()) {
                        pendingAngle = PendingAngle(angle.id, angle.vertex, angle.armA)
                        draft = angle
                    } else if (angle != null && pendingAngle != null && moved) {
                        onAdd(angle)
                        onSelect(angle.id)
                        pendingAngle = null
                        draft = null
                    } else if (pendingAngle == null) {
                        // Accidental tap with no drag: discard the degenerate draft instead of
                        // leaving a zero-length "0°" ghost on the frame.
                        draft = null
                    }
                }

                else -> {
                    if (completed != null && shapeIsLargeEnough(
                            completed,
                            imageRect,
                            MIN_SHAPE_SIZE_DP.dp.toPx(),
                        )
                    ) {
                        onAdd(completed)
                        onSelect(completed.id)
                    }
                    draft = null
                }
            }
        }
    }

    Canvas(modifier = modifier.then(gestureModifier)) {
        val imageRect = fittedImageRect(size, bitmapWidth, bitmapHeight)
        carriedAnnotations.forEach { shape ->
            drawAnnotation(shape, imageRect, alpha = 0.34f, angleTextPaint = angleTextPaint)
        }

        val draftId = draft?.id
        annotations.forEach { shape ->
            val rendered = if (shape.id == draftId) draft ?: shape else shape
            drawAnnotation(rendered, imageRect, alpha = 1f, angleTextPaint = angleTextPaint)
        }
        draft?.takeIf { candidate -> annotations.none { it.id == candidate.id } }?.let { candidate ->
            drawAnnotation(candidate, imageRect, alpha = 1f, angleTextPaint = angleTextPaint)
        }

        val selected = draft?.takeIf { it.id == selectedAnnotationId }
            ?: annotations.firstOrNull { it.id == selectedAnnotationId }
        if (selected != null) drawSelectionHandles(selected, imageRect)
    }
}

private fun DrawScope.drawAnnotation(
    shape: AnnotationShape,
    imageRect: Rect,
    alpha: Float,
    angleTextPaint: Paint,
) {
    val color = Color(shape.style.colorArgb).copy(alpha = alpha)
    val stroke = shape.style.strokeWidthDp.dp.toPx()

    when (shape) {
        is LineAnnotation -> drawLine(
            color = color,
            start = imageRect.toOffset(shape.start),
            end = imageRect.toOffset(shape.end),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        is AngleAnnotation -> {
            val vertex = imageRect.toOffset(shape.vertex)
            drawLine(color, vertex, imageRect.toOffset(shape.armA), stroke, StrokeCap.Round)
            drawLine(color, vertex, imageRect.toOffset(shape.armB), stroke, StrokeCap.Round)
            angleTextPaint.color = android.graphics.Color.argb(
                (255 * alpha).roundToInt(),
                255,
                255,
                255,
            )
            angleTextPaint.textSize = 13.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(
                "${shape.angleDegrees(imageRect.width, imageRect.height).roundToInt()}°",
                vertex.x + 9.dp.toPx(),
                vertex.y - 9.dp.toPx(),
                angleTextPaint,
            )
        }

        is PlumbAnnotation -> {
            if (shape.orientation == PlumbOrientation.VERTICAL) {
                val x = imageRect.left + imageRect.width * shape.position
                drawLine(color, Offset(x, imageRect.top), Offset(x, imageRect.bottom), stroke, StrokeCap.Round)
            } else {
                val y = imageRect.top + imageRect.height * shape.position
                drawLine(color, Offset(imageRect.left, y), Offset(imageRect.right, y), stroke, StrokeCap.Round)
            }
        }

        is BoxAnnotation -> {
            val rect = imageRect.fromCorners(shape.cornerA, shape.cornerB)
            drawRect(color = color, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke))
        }

        is CircleAnnotation -> {
            val rect = imageRect.fromCorners(shape.cornerA, shape.cornerB)
            drawOval(color = color, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke))
        }

        is FreehandAnnotation -> {
            if (shape.points.size >= 2) {
                val path = Path().apply {
                    val first = imageRect.toOffset(shape.points.first())
                    moveTo(first.x, first.y)
                    shape.points.drop(1).forEach { point ->
                        val offset = imageRect.toOffset(point)
                        lineTo(offset.x, offset.y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private fun DrawScope.drawSelectionHandles(shape: AnnotationShape, imageRect: Rect) {
    shape.handles().forEach { handle ->
        val center = imageRect.toOffset(handle.point)
        drawCircle(
            color = SwingFrameColors.Background.copy(alpha = 0.92f),
            radius = 8.dp.toPx(),
            center = center,
        )
        drawCircle(
            color = SwingFrameColors.Accent,
            radius = 5.dp.toPx(),
            center = center,
        )
        drawCircle(
            color = SwingFrameColors.TextPrimary,
            radius = 2.dp.toPx(),
            center = center,
        )
    }
}

private fun shapeIsLargeEnough(
    shape: AnnotationShape,
    imageRect: Rect,
    minimumSizePx: Float,
): Boolean = when (shape) {
    is LineAnnotation -> imageRect.pixelDistance(shape.start, imageRect.toOffset(shape.end)) >= minimumSizePx
    is AngleAnnotation -> true
    is PlumbAnnotation -> true
    is BoxAnnotation -> imageRect.pixelDistance(shape.cornerA, imageRect.toOffset(shape.cornerB)) >= minimumSizePx
    is CircleAnnotation -> imageRect.pixelDistance(shape.cornerA, imageRect.toOffset(shape.cornerB)) >= minimumSizePx
    is FreehandAnnotation -> shape.points.size >= 2
}

private fun fittedImageRect(container: Size, bitmapWidth: Int, bitmapHeight: Int): Rect {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || container.width <= 0f || container.height <= 0f) {
        return Rect(Offset.Zero, container)
    }
    val scale = min(container.width / bitmapWidth, container.height / bitmapHeight)
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val left = (container.width - width) / 2f
    val top = (container.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun Rect.toOffset(point: NormalizedPoint): Offset = Offset(
    x = left + width * point.x,
    y = top + height * point.y,
)

private fun Rect.toNormalized(offset: Offset): NormalizedPoint = NormalizedPoint(
    x = ((offset.x - left) / width.coerceAtLeast(1f)).coerceIn(0f, 1f),
    y = ((offset.y - top) / height.coerceAtLeast(1f)).coerceIn(0f, 1f),
)

private fun Rect.fromCorners(a: NormalizedPoint, b: NormalizedPoint): Rect {
    val first = toOffset(a)
    val second = toOffset(b)
    return Rect(
        left = min(first.x, second.x),
        top = min(first.y, second.y),
        right = max(first.x, second.x),
        bottom = max(first.y, second.y),
    )
}

private fun Rect.pixelDistance(point: NormalizedPoint, offset: Offset): Float {
    val pointOffset = toOffset(point)
    return hypot(pointOffset.x - offset.x, pointOffset.y - offset.y)
}

private const val HANDLE_HIT_RADIUS_DP = 18f
private const val SHAPE_HIT_RADIUS_DP = 12f
private const val DRAG_SLOP_DP = 3f
private const val FREEHAND_STEP_DP = 3f
private const val MAX_FREEHAND_POINTS = 1_500
private const val MIN_SHAPE_SIZE_DP = 8f
