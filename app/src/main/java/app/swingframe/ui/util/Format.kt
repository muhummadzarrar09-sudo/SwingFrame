package app.swingframe.ui.util

import java.util.Locale

/**
 * Formats milliseconds as MM:SS.cc, e.g. 83_450 -> "01:23.45".
 * The separator is pinned via Locale.US so it is always "." regardless of device locale.
 */
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (ms % 1000) / 10 // two digits
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, milliseconds)
}
