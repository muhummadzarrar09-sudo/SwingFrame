package app.swingframe.annotation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AnnotationHandle(val index: Int, val point: NormalizedPoint)

data class AnnotationBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

fun AnnotationShape.handles(): List<AnnotationHandle> = when (this) {
    is LineAnnotation -> listOf(AnnotationHandle(0, start), AnnotationHandle(1, end))
    is AngleAnnotation -> listOf(
        AnnotationHandle(0, vertex),
        AnnotationHandle(1, armA),
        AnnotationHandle(2, armB),
    )
    is PlumbAnnotation -> listOf(
        AnnotationHandle(
            0,
            if (orientation == PlumbOrientation.VERTICAL) NormalizedPoint(position, 0.5f)
            else NormalizedPoint(0.5f, position),
        ),
    )
    is BoxAnnotation -> listOf(AnnotationHandle(0, cornerA), AnnotationHandle(1, cornerB))
    is CircleAnnotation -> listOf(AnnotationHandle(0, cornerA), AnnotationHandle(1, cornerB))
    is FreehandAnnotation -> buildList {
        points.firstOrNull()?.let { add(AnnotationHandle(0, it)) }
        if (points.size > 1) add(AnnotationHandle(1, points.last()))
    }
}

fun AnnotationShape.withHandle(index: Int, point: NormalizedPoint): AnnotationShape {
    val safe = point.clamped()
    return when (this) {
        is LineAnnotation -> if (index == 0) copy(start = safe) else copy(end = safe)
        is AngleAnnotation -> when (index) {
            0 -> copy(vertex = safe)
            1 -> copy(armA = safe)
            else -> copy(armB = safe)
        }
        is PlumbAnnotation -> copy(
            position = if (orientation == PlumbOrientation.VERTICAL) safe.x else safe.y,
        )
        is BoxAnnotation -> if (index == 0) copy(cornerA = safe) else copy(cornerB = safe)
        is CircleAnnotation -> if (index == 0) copy(cornerA = safe) else copy(cornerB = safe)
        is FreehandAnnotation -> {
            if (points.isEmpty()) this else {
                val mutable = points.toMutableList()
                if (index == 0) mutable[0] = safe else mutable[mutable.lastIndex] = safe
                copy(points = mutable)
            }
        }
    }
}

/** Rigid translation: one clamped delta is applied to every point, preserving shape dimensions. */
fun AnnotationShape.translated(dx: Float, dy: Float): AnnotationShape {
    if (this is PlumbAnnotation) {
        val delta = if (orientation == PlumbOrientation.VERTICAL) dx else dy
        return copy(position = (position + delta).coerceIn(0f, 1f))
    }

    val bounds = bounds()
    val safeDx = dx.coerceIn(-bounds.left, 1f - bounds.right)
    val safeDy = dy.coerceIn(-bounds.top, 1f - bounds.bottom)
    fun NormalizedPoint.shift() = NormalizedPoint(x + safeDx, y + safeDy)

    return when (this) {
        is LineAnnotation -> copy(start = start.shift(), end = end.shift())
        is AngleAnnotation -> copy(vertex = vertex.shift(), armA = armA.shift(), armB = armB.shift())
        is BoxAnnotation -> copy(cornerA = cornerA.shift(), cornerB = cornerB.shift())
        is CircleAnnotation -> copy(cornerA = cornerA.shift(), cornerB = cornerB.shift())
        is FreehandAnnotation -> copy(points = points.map { it.shift() })
        is PlumbAnnotation -> this // handled above
    }
}

fun AnnotationShape.bounds(): AnnotationBounds {
    val points = when (this) {
        is LineAnnotation -> listOf(start, end)
        is AngleAnnotation -> listOf(vertex, armA, armB)
        is PlumbAnnotation -> if (orientation == PlumbOrientation.VERTICAL) {
            listOf(NormalizedPoint(position, 0f), NormalizedPoint(position, 1f))
        } else {
            listOf(NormalizedPoint(0f, position), NormalizedPoint(1f, position))
        }
        is BoxAnnotation -> listOf(cornerA, cornerB)
        is CircleAnnotation -> listOf(cornerA, cornerB)
        is FreehandAnnotation -> points
    }
    if (points.isEmpty()) return AnnotationBounds(0f, 0f, 0f, 0f)
    return AnnotationBounds(
        left = points.minOf { it.x },
        top = points.minOf { it.y },
        right = points.maxOf { it.x },
        bottom = points.maxOf { it.y },
    )
}

fun AnnotationShape.withStyle(style: AnnotationStyle): AnnotationShape = when (this) {
    is LineAnnotation -> copy(style = style)
    is AngleAnnotation -> copy(style = style)
    is PlumbAnnotation -> copy(style = style)
    is BoxAnnotation -> copy(style = style)
    is CircleAnnotation -> copy(style = style)
    is FreehandAnnotation -> copy(style = style)
}

/** Pixel/aspect-correct hit distance. */
fun AnnotationShape.distanceTo(
    point: NormalizedPoint,
    pixelWidth: Float = 1f,
    pixelHeight: Float = 1f,
): Float = when (this) {
    is LineAnnotation -> segmentDistance(point, start, end, pixelWidth, pixelHeight)
    is AngleAnnotation -> min(
        segmentDistance(point, vertex, armA, pixelWidth, pixelHeight),
        segmentDistance(point, vertex, armB, pixelWidth, pixelHeight),
    )
    is PlumbAnnotation -> if (orientation == PlumbOrientation.VERTICAL) {
        abs(point.x - position) * pixelWidth
    } else {
        abs(point.y - position) * pixelHeight
    }
    is BoxAnnotation -> rectangleEdgeDistance(point, cornerA, cornerB, pixelWidth, pixelHeight)
    is CircleAnnotation -> ellipseEdgeDistance(point, cornerA, cornerB, pixelWidth, pixelHeight)
    is FreehandAnnotation -> points.zipWithNext { first, second ->
        segmentDistance(point, first, second, pixelWidth, pixelHeight)
    }.minOrNull() ?: Float.MAX_VALUE
}

fun AngleAnnotation.angleDegrees(): Float {
    val ax = armA.x - vertex.x
    val ay = armA.y - vertex.y
    val bx = armB.x - vertex.x
    val by = armB.y - vertex.y
    val lengthA = sqrt(ax * ax + ay * ay)
    val lengthB = sqrt(bx * bx + by * by)
    if (lengthA < 0.0001f || lengthB < 0.0001f) return 0f
    val cosine = ((ax * bx + ay * by) / (lengthA * lengthB)).coerceIn(-1f, 1f)
    return (acos(cosine) * 180f / PI.toFloat()).coerceIn(0f, 180f)
}

private fun segmentDistance(
    point: NormalizedPoint,
    start: NormalizedPoint,
    end: NormalizedPoint,
    pixelWidth: Float,
    pixelHeight: Float,
): Float {
    val px = point.x * pixelWidth
    val py = point.y * pixelHeight
    val sx = start.x * pixelWidth
    val sy = start.y * pixelHeight
    val ex = end.x * pixelWidth
    val ey = end.y * pixelHeight
    val vx = ex - sx
    val vy = ey - sy
    val wx = px - sx
    val wy = py - sy
    val lengthSquared = vx * vx + vy * vy
    if (lengthSquared < 0.000001f) return hypot(px - sx, py - sy)
    val t = ((wx * vx + wy * vy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(px - (sx + t * vx), py - (sy + t * vy))
}

private fun rectangleEdgeDistance(
    point: NormalizedPoint,
    a: NormalizedPoint,
    b: NormalizedPoint,
    pixelWidth: Float,
    pixelHeight: Float,
): Float {
    val left = min(a.x, b.x)
    val right = max(a.x, b.x)
    val top = min(a.y, b.y)
    val bottom = max(a.y, b.y)
    val corners = listOf(
        NormalizedPoint(left, top),
        NormalizedPoint(right, top),
        NormalizedPoint(right, bottom),
        NormalizedPoint(left, bottom),
    )
    return corners.zip(corners.drop(1) + corners.first()).minOf { (first, second) ->
        segmentDistance(point, first, second, pixelWidth, pixelHeight)
    }
}

private fun ellipseEdgeDistance(
    point: NormalizedPoint,
    a: NormalizedPoint,
    b: NormalizedPoint,
    pixelWidth: Float,
    pixelHeight: Float,
): Float {
    val centerX = (a.x + b.x) / 2f
    val centerY = (a.y + b.y) / 2f
    val radiusX = abs(a.x - b.x) / 2f
    val radiusY = abs(a.y - b.y) / 2f
    if (radiusX < 0.0001f || radiusY < 0.0001f) {
        return hypot((point.x - a.x) * pixelWidth, (point.y - a.y) * pixelHeight)
    }
    val nx = (point.x - centerX) / radiusX
    val ny = (point.y - centerY) / radiusY
    val radial = sqrt(nx * nx + ny * ny)
    return abs(radial - 1f) * min(radiusX * pixelWidth, radiusY * pixelHeight)
}

private fun hypot(x: Float, y: Float): Float = sqrt(x * x + y * y)
