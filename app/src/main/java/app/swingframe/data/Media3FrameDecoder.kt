package app.swingframe.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.inspector.frame.FrameExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Persistent exact-frame preview decoder.
 *
 * Media3 FrameExtractor currently prefers software decoding by default. SwingFrame explicitly
 * starts with the platform hardware selector for responsive scrubbing, then permanently falls
 * back to Media3's software-preferred selector if the device codec rejects the source.
 * All calls stay on the main application thread, as required by FrameExtractor.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class Media3FrameDecoder(
    context: Context,
    uri: Uri,
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val mediaItem = MediaItem.fromUri(uri)

    private var usingSoftwareFallback = false
    private var extractor = createExtractor(MediaCodecSelector.DEFAULT)

    suspend fun decodePreviewFrame(timestampUs: Long): Bitmap {
        val source = decodeSourceFrame(timestampUs)
        // FrameExtractor pins decode calls to the application thread, but bitmap scaling is pure
        // CPU work and must not block that thread during rapid scrubbing.
        return withContext(Dispatchers.Default) { source.scaledForPreview(MAX_PREVIEW_EDGE_PX) }
    }

    suspend fun decodeSourceFrame(timestampUs: Long): Bitmap {
        assertApplicationThread()
        val positionMs = timestampUs.coerceAtLeast(0L) / 1_000L

        return try {
            extractor.getFrame(positionMs).await().bitmap
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (hardwareFailure: Exception) {
            if (usingSoftwareFallback) throw hardwareFailure
            switchToSoftwareFallback()
            extractor.getFrame(positionMs).await().bitmap
        }
    }

    fun isUsingSoftwareFallback(): Boolean = usingSoftwareFallback

    override fun close() {
        assertApplicationThread()
        extractor.close()
    }

    private fun createExtractor(codecSelector: MediaCodecSelector): FrameExtractor =
        FrameExtractor.Builder(applicationContext, mediaItem)
            .setMediaCodecSelector(codecSelector)
            .setSeekParameters(SeekParameters.EXACT)
            .build()

    private fun switchToSoftwareFallback() {
        assertApplicationThread()
        extractor.close()
        usingSoftwareFallback = true
        extractor = createExtractor(MediaCodecSelector.PREFER_SOFTWARE)
    }

    private fun assertApplicationThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "FrameExtractor must be accessed from SwingFrame's main application thread."
        }
    }

    private fun Bitmap.scaledForPreview(maxEdge: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxEdge || longest <= 0) return this
        val ratio = maxEdge.toFloat() / longest.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * ratio).roundToInt().coerceAtLeast(1),
            (height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
        // createScaledBitmap allocates a second pixel buffer. FrameExtractor transfers ownership
        // of the source bitmap to us, so retaining both makes repeated 4K seeks spike memory.
        if (scaled !== this && !isRecycled) recycle()
        return scaled
    }

    private companion object {
        const val MAX_PREVIEW_EDGE_PX = 1_920
    }
}
