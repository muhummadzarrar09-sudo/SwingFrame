package app.swingframe.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `formats milliseconds as minutes seconds centiseconds`() {
        assertEquals("01:23.45", formatTime(83_450))
        assertEquals("00:00.00", formatTime(0L))
        assertEquals("10:00.00", formatTime(600_000L))
        assertEquals("00:00.99", formatTime(999L))
    }
}
