package app.swingframe.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameIndexerTest {
    @Test
    fun presentationOrder_preservesSeparateSamplesWithDuplicatePts() {
        assertEquals(
            listOf(0L, 33_333L, 33_333L, 66_667L),
            presentationOrderTimestamps(listOf(66_667L, 33_333L, 0L, 33_333L)),
        )
    }
}
