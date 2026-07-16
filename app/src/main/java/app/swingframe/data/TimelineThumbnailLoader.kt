package app.swingframe.data

import android.content.Context
import android.media.MediaMetadataRetriever
import app.swingframe.model.IndexedVideo
import app.swingframe.model.TimelineThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Loads a small, evenly sampled visual index for the timeline.
 *
 * Thumbnails are intentionally independent from the exact-frame preview decoder: they may use
 * nearby sync frames because they provide orientation, while the amber playhead and frame readout
 * continue to address the exact indexed presentation timestamp.
 */
class TimelineThumbnailLoader(private val context: Context) {

    suspend fun load(
        video: IndexedVideo,
        maximumCount: Int = 12,
        onThumbnail: (TimelineThumbnail) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (video.totalFrames <= 0) return@withContext

        val sampleCount = minOf(maximumCount.coerceAtLeast(2), video.totalFrames)
        val indices = evenlySpacedFrameIndices(video.totalFrames, sampleCount)
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, video.source.uri)
            indices.forEach { frameIndex ->
                currentCoroutineContext().ensureActive()
                val timestampUs = video.frameTimestampsUs[frameIndex]
                val bitmap = retriever.getScaledFrameAtTime(
                    timestampUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                )
                if (bitmap != null) {
                    onThumbnail(TimelineThumbnail(frameIndex, bitmap))
                }
            }
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val THUMBNAIL_WIDTH = 240
        const val THUMBNAIL_HEIGHT = 144
    }
}

internal fun evenlySpacedFrameIndices(totalFrames: Int, count: Int): List<Int> {
    if (totalFrames <= 0 || count <= 0) return emptyList()
    if (totalFrames == 1 || count == 1) return listOf(0)
    return List(count) { slot ->
        ((slot.toLong() * (totalFrames - 1)) / (count - 1)).toInt()
    }.distinct()
}
