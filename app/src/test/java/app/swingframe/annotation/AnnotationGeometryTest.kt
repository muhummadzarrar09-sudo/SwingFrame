package app.swingframe.annotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationGeometryTest {
    private val style = AnnotationStyle(colorArgb = -1, strokeWidthDp = 3f)

    @Test
    fun angleDegrees_returnsRightAngle() {
        val angle = AngleAnnotation(
            vertex = NormalizedPoint(0.5f, 0.5f),
            armA = NormalizedPoint(0.5f, 0.1f),
            armB = NormalizedPoint(0.9f, 0.5f),
            style = style,
        )
        assertEquals(90f, angle.angleDegrees(), 0.01f)
    }

    @Test
    fun lineHandle_replacesRequestedEndpoint() {
        val line = LineAnnotation(
            start = NormalizedPoint(0.1f, 0.1f),
            end = NormalizedPoint(0.8f, 0.8f),
            style = style,
        )
        val changed = line.withHandle(1, NormalizedPoint(0.7f, 0.2f)) as LineAnnotation
        assertEquals(NormalizedPoint(0.1f, 0.1f), changed.start)
        assertEquals(NormalizedPoint(0.7f, 0.2f), changed.end)
    }

    @Test
    fun translatedShape_staysWithinCanvasWithoutChangingLength() {
        val line = LineAnnotation(
            start = NormalizedPoint(0.8f, 0.8f),
            end = NormalizedPoint(0.9f, 0.9f),
            style = style,
        )
        val moved = line.translated(0.4f, 0.4f) as LineAnnotation
        assertTrue(moved.start.x in 0f..1f)
        assertTrue(moved.start.y in 0f..1f)
        assertTrue(moved.end.x in 0f..1f)
        assertTrue(moved.end.y in 0f..1f)
        assertEquals(0.1f, moved.end.x - moved.start.x, 0.0001f)
        assertEquals(0.1f, moved.end.y - moved.start.y, 0.0001f)
    }

    @Test
    fun distanceTo_usesPixelAspectRatio() {
        val vertical = LineAnnotation(
            start = NormalizedPoint(0.5f, 0.1f),
            end = NormalizedPoint(0.5f, 0.9f),
            style = style,
        )
        val point = NormalizedPoint(0.6f, 0.5f)
        assertEquals(20f, vertical.distanceTo(point, pixelWidth = 200f, pixelHeight = 800f), 0.01f)
    }
}
