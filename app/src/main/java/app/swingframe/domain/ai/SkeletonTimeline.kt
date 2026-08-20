package app.swingframe.domain.ai

import kotlin.math.abs

/**
 * Immutable, pre-sorted index over a skeleton map for O(log n) nearest-frame lookups.
 *
 * The overlay is recomposed at up to ~60 Hz during playback; a linear scan of the whole
 * map on every recomposition would be wasteful, so frame times are sorted once when the
 * analysis completes and lookups use binary search.
 */
class SkeletonTimeline private constructor(
    private val times: LongArray,
    private val skeletons: Map<Long, SwingSkeleton>
) {
    val size: Int
        get() = times.size

    /** Returns the skeleton whose frame time is closest to [positionMs], or null if empty. */
    fun skeletonAt(positionMs: Long): SwingSkeleton? {
        if (times.isEmpty()) return null

        // Binary search for the first index with times[index] >= positionMs.
        var lo = 0
        var hi = times.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (times[mid] < positionMs) lo = mid + 1 else hi = mid - 1
        }

        // The closest frame is one of the two neighbours of the insertion point.
        var bestIndex = -1
        var bestDistance = Long.MAX_VALUE
        for (candidate in lo - 1..lo + 1) {
            if (candidate in times.indices) {
                val distance = abs(times[candidate] - positionMs)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = candidate
                }
            }
        }
        return if (bestIndex == -1) null else skeletons[times[bestIndex]]
    }

    companion object {
        val EMPTY = SkeletonTimeline(LongArray(0), emptyMap())

        fun from(skeletons: Map<Long, SwingSkeleton>): SkeletonTimeline =
            SkeletonTimeline(skeletons.keys.sorted().toLongArray(), skeletons)
    }
}
