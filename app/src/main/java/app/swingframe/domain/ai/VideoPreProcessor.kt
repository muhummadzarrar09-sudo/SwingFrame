package app.swingframe.domain.ai

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Scans a local video, extracts a skeleton for each sampled frame, and runs the
 * auto-flaw heuristics. All heavy work runs on [Dispatchers.Default].
 *
 * The scan is deliberately bounded: frames are downscaled to <= 640 px, sampled at
 * ~10 fps, and capped at [MAX_FRAMES] so a long range-session clip cannot peg the CPU
 * or exhaust memory. Cancellation is respected — a cancelled scan rethrows
 * [CancellationException] and never returns a partial result as if it were complete.
 */
class VideoPreProcessor(
    private val context: Context,
    private val poseAnalyzer: OnDevicePoseAnalyzer,
    private val swingAnalyzer: SwingAnalyzer = SwingAnalyzer()
) {

    /**
     * Returns [SwingAnalysisResult.Success] when the video was read and analyzed (even if no
     * person was detected — that is reported through the report's [AnalysisQuality]), or
     * [SwingAnalysisResult.Failure] when the video itself could not be processed.
     */
    suspend fun processVideo(
        uri: Uri,
        onProgress: (Float) -> Unit
    ): SwingAnalysisResult = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) {
                return@withContext SwingAnalysisResult.Failure(
                    "Could not read the video duration. The file may be corrupt or unsupported."
                )
            }

            // MediaMetadataRetriever returns frames in the ENCODED orientation and does not
            // apply rotation metadata; ExoPlayer rotates during playback, so the rotation
            // must be forwarded to the pose analyzer or the skeleton lands rotated.
            val rotationDegrees = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            // Sample at ~10 fps, downscaled, with a hard cap on the number of frames.
            val rawSteps = durationMs / SAMPLE_STEP_MS
            val stepStride = if (rawSteps > MAX_FRAMES) {
                ((rawSteps + MAX_FRAMES - 1) / MAX_FRAMES).coerceAtLeast(1L)
            } else {
                1L
            }
            val totalSteps = (rawSteps / stepStride).coerceAtLeast(1L)

            val skeletons = mutableMapOf<Long, SwingSkeleton>()
            var processed = 0L
            var step = 0L
            while (step <= rawSteps) {
                coroutineContext.ensureActive() // bail out promptly if the caller cancels

                val currentMs = (step * SAMPLE_STEP_MS).coerceAtMost(durationMs)
                val bitmap = retriever.getScaledFrameAtTime(
                    currentMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    MAX_DECODE_WIDTH,
                    MAX_DECODE_HEIGHT
                )
                if (bitmap != null) {
                    val skeleton = poseAnalyzer.analyzeFrame(bitmap, rotationDegrees)
                    if (skeleton != null) {
                        skeletons[currentMs] = skeleton
                    }
                    if (!bitmap.isRecycled) bitmap.recycle()
                }

                processed++
                onProgress((processed.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f))
                step += stepStride
            }

            val report = swingAnalyzer.analyzeSwing(skeletons)
            SwingAnalysisResult.Success(skeletons, report)
        } catch (e: CancellationException) {
            throw e // never swallow cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Video pre-processing failed for $uri", e)
            SwingAnalysisResult.Failure(
                "Video processing failed: ${e.message ?: e.javaClass.simpleName}. The file may be corrupt or unsupported."
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release MediaMetadataRetriever", e)
            }
        }
    }

    private companion object {
        const val TAG = "VideoPreProcessor"
        const val SAMPLE_STEP_MS = 100L // ~10 fps sampling
        const val MAX_FRAMES = 600L     // 60 s of video at 10 fps
        const val MAX_DECODE_WIDTH = 640
        const val MAX_DECODE_HEIGHT = 640
    }
}
