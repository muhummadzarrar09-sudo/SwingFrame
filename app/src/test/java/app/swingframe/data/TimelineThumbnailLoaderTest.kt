package app.swingframe.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineThumbnailLoaderTest {
    @Test
    fun evenlySpacedIndices_includeFirstAndLastFrames() {
        assertEquals(
            listOf(0, 24, 49, 74, 99),
            evenlySpacedFrameIndices(totalFrames = 100, count = 5),
        )
    }

    @Test
    fun evenlySpacedIndices_doesNotDuplicateShortTimelines() {
        assertEquals(
            listOf(0, 1, 2),
            evenlySpacedFrameIndices(totalFrames = 3, count = 3),
        )
    }
}
