package app.swingframe.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.swingframe.annotation.AngleAnnotation
import app.swingframe.annotation.AnnotationShape
import app.swingframe.annotation.BoxAnnotation
import app.swingframe.annotation.CircleAnnotation
import app.swingframe.annotation.FreehandAnnotation
import app.swingframe.annotation.LineAnnotation
import app.swingframe.annotation.NormalizedPoint
import app.swingframe.annotation.PlumbAnnotation
import app.swingframe.annotation.PlumbOrientation
import app.swingframe.annotation.angleDegrees
import kotlin.math.max
import kotlin.math.min

/** Shared Android-Canvas renderer used by still and Media3 video export. */
object AnnotationBitmapRenderer {
    fun renderCopy(
        source: Bitmap,
        annotations: List<AnnotationShape>,
        carriedAnnotations: List<AnnotationShape> = emptyList(),
    ): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        draw(canvas, output.width, output.height, carriedAnnotations, alpha = 0.34f)
        draw(canvas, output.width, output.height, annotations)
        return output
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        annotations: List<AnnotationShape>,
        alpha: Float = 1f,
    ) {
        // Preview strokes are density-scaled dp over a stage a few hundred dp wide. Exporting
        // with the same dp model at a 3x reference density (1 dp = min/360 px) keeps burned
        // strokes and text proportional to what the preview showed instead of several times
        // thinner. Device golden tests remain the final parity check.
        val scale = (min(width, height) / 360f).coerceIn(1f, 8f)
        annotations.forEach { shape -> drawShape(canvas, width, height, shape, alpha, scale) }
    }

    private fun drawShape(
        canvas: Canvas,
        width: Int,
        height: Int,
        shape: AnnotationShape,
        alpha: Float,
        scale: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(shape.style.colorArgb, alpha)
            strokeWidth = shape.style.strokeWidthDp * scale
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        when (shape) {
            is LineAnnotation -> canvas.drawLine(
                shape.start.x * width,
                shape.start.y * height,
                shape.end.x * width,
                shape.end.y * height,
                paint,
            )
            is AngleAnnotation -> {
                val vx = shape.vertex.x * width
                val vy = shape.vertex.y * height
                canvas.drawLine(vx, vy, shape.armA.x * width, shape.armA.y * height, paint)
                canvas.drawLine(vx, vy, shape.armB.x * width, shape.armB.y * height, paint)
                paint.style = Paint.Style.FILL
                // Matches the preview's 13dp label at the shared scale, with the same 9dp offset.
                paint.textSize = 13f * scale
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText(
                    "${shape.angleDegrees(width.toFloat(), height.toFloat()).toInt()}°",
                    vx + 9f * scale,
                    vy - 9f * scale,
                    paint,
                )
            }
            is PlumbAnnotation -> {
                if (shape.orientation == PlumbOrientation.VERTICAL) {
                    val x = shape.position * width
                    canvas.drawLine(x, 0f, x, height.toFloat(), paint)
                } else {
                    val y = shape.position * height
                    canvas.drawLine(0f, y, width.toFloat(), y, paint)
                }
            }
            is BoxAnnotation -> canvas.drawRect(rect(shape.cornerA, shape.cornerB, width, height), paint)
            is CircleAnnotation -> canvas.drawOval(rect(shape.cornerA, shape.cornerB, width, height), paint)
            is FreehandAnnotation -> {
                if (shape.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(shape.points.first().x * width, shape.points.first().y * height)
                        shape.points.drop(1).forEach { lineTo(it.x * width, it.y * height) }
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun rect(a: NormalizedPoint, b: NormalizedPoint, width: Int, height: Int): RectF = RectF(
        min(a.x, b.x) * width,
        min(a.y, b.y) * height,
        max(a.x, b.x) * width,
        max(a.y, b.y) * height,
    )

    private fun withAlpha(argb: Int, alpha: Float): Int = Color.argb(
        (Color.alpha(argb) * alpha.coerceIn(0f, 1f)).toInt(),
        Color.red(argb),
        Color.green(argb),
        Color.blue(argb),
    )
}
