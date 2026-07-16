package app.swingframe.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportTimelineMapperTest {
    private val timestamps = listOf(0L, 16_667L, 33_334L, 50_001L)

    @Test
    fun slowMotionOutput_mapsBackToSourceTime() {
        assertEquals(
            25_000L,
            ExportTimelineMapper.sourceTimeUs(
                sourceStartUs = 0L,
                outputPresentationTimeUs = 100_000L,
                speed = 0.25f,
            ),
        )
    }

    @Test
    fun nearestFrame_usesActualTimestamps() {
        assertEquals(1, ExportTimelineMapper.nearestFrameIndex(timestamps, 20_000L))
        assertEquals(2, ExportTimelineMapper.nearestFrameIndex(timestamps, 31_000L))
    }

    @Test
    fun endExclusive_usesNextFrameBoundary() {
        assertEquals(50_001L, ExportTimelineMapper.endExclusiveTimestampUs(timestamps, 2))
    }
}
