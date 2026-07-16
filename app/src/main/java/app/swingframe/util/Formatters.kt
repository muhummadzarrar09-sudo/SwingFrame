package app.swingframe.util

import kotlin.math.roundToInt

fun formatTimestampUs(timestampUs: Long): String {
    val safeUs = timestampUs.coerceAtLeast(0L)
    val totalCentiseconds = safeUs / 10_000L
    val minutes = totalCentiseconds / 6_000L
    val seconds = (totalCentiseconds / 100L) % 60L
    val centiseconds = totalCentiseconds % 100L
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}

fun formatDurationUs(durationUs: Long): String = formatTimestampUs(durationUs)

fun formatFps(fps: Float): String {
    val rounded = fps.roundToInt()
    return if (kotlin.math.abs(fps - rounded) < 0.05f) {
        "$rounded FPS"
    } else {
        "%.2f FPS".format(fps)
    }
}

fun formatResolution(width: Int, height: Int, rotationDegrees: Int): String {
    val swap = rotationDegrees == 90 || rotationDegrees == 270
    return if (swap) "$height × $width" else "$width × $height"
}

fun formatBitrate(bitsPerSecond: Long?): String {
    if (bitsPerSecond == null || bitsPerSecond <= 0L) return "Unknown bitrate"
    return if (bitsPerSecond >= 1_000_000L) {
        "%.1f Mbps".format(bitsPerSecond / 1_000_000f)
    } else {
        "${bitsPerSecond / 1_000L} Kbps"
    }
}
