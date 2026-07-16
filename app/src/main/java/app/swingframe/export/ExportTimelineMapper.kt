package app.swingframe.export

/** Pure source/output timestamp mapping used by slow-motion overlays and tests. */
object ExportTimelineMapper {
    fun sourceTimeUs(sourceStartUs: Long, outputPresentationTimeUs: Long, speed: Float): Long =
        sourceStartUs + (outputPresentationTimeUs * speed.coerceIn(0.1f, 1f)).toLong()

    fun nearestFrameIndex(timestamps: List<Long>, targetUs: Long): Int {
        require(timestamps.isNotEmpty())
        val found = timestamps.binarySearch(targetUs)
        if (found >= 0) return found
        val insertion = -found - 1
        if (insertion <= 0) return 0
        if (insertion >= timestamps.size) return timestamps.lastIndex
        val before = timestamps[insertion - 1]
        val after = timestamps[insertion]
        return if (targetUs - before <= after - targetUs) insertion - 1 else insertion
    }

    fun endExclusiveTimestampUs(timestamps: List<Long>, endFrameIndex: Int): Long {
        require(timestamps.isNotEmpty())
        val safeEnd = endFrameIndex.coerceIn(timestamps.indices)
        if (safeEnd < timestamps.lastIndex) return timestamps[safeEnd + 1]
        val intervals = timestamps.zipWithNext { first, second -> second - first }
            .filter { it > 0L }
            .sorted()
        val fallbackInterval = if (intervals.isEmpty()) 33_333L else intervals[intervals.size / 2]
        return timestamps[safeEnd] + fallbackInterval
    }
}
