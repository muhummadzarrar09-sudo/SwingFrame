package app.swingframe.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun timestamp_formatsCentiseconds() {
        assertEquals("00:02.90", formatTimestampUs(2_900_000L))
        assertEquals("01:03.45", formatTimestampUs(63_450_000L))
    }

    @Test
    fun resolution_accountsForRotation() {
        assertEquals("1920 × 1080", formatResolution(1920, 1080, 0))
        assertEquals("1080 × 1920", formatResolution(1920, 1080, 90))
    }

    @Test
    fun fps_avoidsUnnecessaryDecimals() {
        assertEquals("60 FPS", formatFps(60.01f))
        assertEquals("59.94 FPS", formatFps(59.94f))
    }
}
