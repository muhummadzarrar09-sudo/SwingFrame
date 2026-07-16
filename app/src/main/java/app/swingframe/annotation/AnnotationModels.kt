package app.swingframe.annotation

import java.util.UUID

/** Active canvas interaction. Selection is also the viewport/navigation mode. */
enum class AnnotationTool {
    SELECT,
    LINE,
    ANGLE,
    PLUMB_VERTICAL,
    PLUMB_HORIZONTAL,
    BOX,
    CIRCLE,
    FREEHAND,
}

data class NormalizedPoint(
    val x: Float,
    val y: Float,
) {
    fun clamped(): NormalizedPoint = NormalizedPoint(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
    )
}

data class AnnotationStyle(
    val colorArgb: Int,
    val strokeWidthDp: Float,
)

sealed interface AnnotationShape {
    val id: String
    val style: AnnotationStyle
}

data class LineAnnotation(
    override val id: String = newAnnotationId(),
    val start: NormalizedPoint,
    val end: NormalizedPoint,
    override val style: AnnotationStyle,
) : AnnotationShape

data class AngleAnnotation(
    override val id: String = newAnnotationId(),
    val vertex: NormalizedPoint,
    val armA: NormalizedPoint,
    val armB: NormalizedPoint,
    override val style: AnnotationStyle,
) : AnnotationShape

enum class PlumbOrientation { VERTICAL, HORIZONTAL }

data class PlumbAnnotation(
    override val id: String = newAnnotationId(),
    val orientation: PlumbOrientation,
    /** X for vertical, Y for horizontal. */
    val position: Float,
    override val style: AnnotationStyle,
) : AnnotationShape

data class BoxAnnotation(
    override val id: String = newAnnotationId(),
    val cornerA: NormalizedPoint,
    val cornerB: NormalizedPoint,
    override val style: AnnotationStyle,
) : AnnotationShape

data class CircleAnnotation(
    override val id: String = newAnnotationId(),
    val cornerA: NormalizedPoint,
    val cornerB: NormalizedPoint,
    override val style: AnnotationStyle,
) : AnnotationShape

data class FreehandAnnotation(
    override val id: String = newAnnotationId(),
    val points: List<NormalizedPoint>,
    override val style: AnnotationStyle,
) : AnnotationShape

internal fun newAnnotationId(): String = UUID.randomUUID().toString()
