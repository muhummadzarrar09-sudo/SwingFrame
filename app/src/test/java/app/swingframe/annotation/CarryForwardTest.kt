package app.swingframe.annotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarryForwardTest {
    private fun shape(id: String) = LineAnnotation(
        id = id,
        start = NormalizedPoint(0.1f, 0.1f),
        end = NormalizedPoint(0.4f, 0.4f),
        style = AnnotationStyle(colorArgb = -1, strokeWidthDp = 3f),
    )

    @Test
    fun nearestPastFrame_winsOverFutureFrames() {
        val map = mapOf(
            8 to listOf(shape("past")),
            12 to listOf(shape("future")),
        )
        val carried = carriedAnnotationsFor(map, frameIndex = 10)
        assertEquals(listOf("past"), carried.map { it.id })
    }

    @Test
    fun futureFrame_isUsedWhenNoPastFrameHasAnnotations() {
        val map = mapOf(12 to listOf(shape("future")))
        val carried = carriedAnnotationsFor(map, frameIndex = 10)
        assertEquals(listOf("future"), carried.map { it.id })
    }

    @Test
    fun emptyFrames_areSkipped() {
        val map = mapOf(
            9 to emptyList(),
            8 to listOf(shape("older")),
        )
        val carried = carriedAnnotationsFor(map, frameIndex = 10)
        assertEquals(listOf("older"), carried.map { it.id })
    }

    @Test
    fun radiusBoundary_isInclusive_andBeyondIsExcluded() {
        val atBoundary = mapOf(7 to listOf(shape("edge")))
        assertEquals(listOf("edge"), carriedAnnotationsFor(atBoundary, 10).map { it.id })

        val beyond = mapOf(6 to listOf(shape("far")))
        assertTrue(carriedAnnotationsFor(beyond, 10).isEmpty())
    }

    @Test
    fun noNearbyAnnotations_returnsEmpty() {
        assertTrue(carriedAnnotationsFor(emptyMap(), 10).isEmpty())
    }
}
