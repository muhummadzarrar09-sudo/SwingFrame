package app.swingframe.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import app.swingframe.model.IndexedVideo
import app.swingframe.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FrameIndexer(private val context: Context) {

    suspend fun index(
        source: VideoSource,
        onProgress: (Float) -> Unit,
    ): IndexedVideo = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, source.uri, null)
            val trackIndex = findVideoTrack(extractor)
            if (trackIndex < 0) {
                throw IllegalArgumentException("No decodable video track was found.")
            }
            extractor.selectTrack(trackIndex)

            val timestamps = ArrayList<Long>(source.metadata.estimatedFrameCount ?: 512)
            var scanned = 0
            while (true) {
                if (scanned % 64 == 0) currentCoroutineContext().ensureActive()
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0L) break
                timestamps += sampleTimeUs
                scanned++

                if (scanned % 12 == 0) {
                    val ratio = sampleTimeUs.toFloat() / source.metadata.durationUs.toFloat()
                    onProgress(ratio.coerceIn(0.02f, 0.98f))
                }
                if (!extractor.advance()) break
            }

            val orderedTimestamps = timestamps.distinct().sorted()
            if (orderedTimestamps.isEmpty()) {
                throw IllegalArgumentException(
                    "SwingFrame could read the video metadata but could not index its frames.",
                )
            }

            onProgress(1f)
            val timing = inspectTiming(orderedTimestamps, source.metadata.declaredFps)
            IndexedVideo(
                source = source,
                frameTimestampsUs = orderedTimestamps,
                detectedFps = timing.first,
                isVariableFrameRate = timing.second,
            )
        } finally {
            extractor.release()
        }
    }

    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) return index
        }
        return -1
    }

    private fun inspectTiming(
        timestampsUs: List<Long>,
        fallbackFps: Float?,
    ): Pair<Float, Boolean> {
        if (timestampsUs.size < 2) return (fallbackFps ?: 1f) to false

        val intervals = timestampsUs.zipWithNext { first, second -> second - first }
            .filter { it > 0L }
            .sorted()
        if (intervals.isEmpty()) return (fallbackFps ?: 1f) to false

        val medianInterval = intervals[intervals.size / 2].toDouble()
        val medianFps = (1_000_000.0 / medianInterval).toFloat()
        val meanInterval = intervals.average()
        val meanDeviation = intervals.sumOf { abs(it - meanInterval) } / intervals.size
        val variable = meanDeviation / meanInterval > 0.035
        return medianFps to variable
    }
}
