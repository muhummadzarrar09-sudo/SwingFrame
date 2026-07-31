package app.swingframe.domain.ai

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoPreProcessor(
    private val context: Context,
    private val poseAnalyzer: OnDevicePoseAnalyzer,
    private val swingAnalyzer: SwingAnalyzer = SwingAnalyzer()
) {
    /**
     * Scans the entire video frame-by-frame, caches the skeletal map,
     * and runs the Auto-Flaw Detection heuristics.
     */
    suspend fun processVideo(
        uri: Uri,
        onProgress: (Float) -> Unit
    ): Pair<Map<Long, SwingSkeleton>, SwingReport> = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        val skeletons = mutableMapOf<Long, SwingSkeleton>()

        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (durationMs > 0) {
                // Extract at ~30 FPS (every 33ms)
                val stepMs = 33L
                val totalSteps = (durationMs / stepMs).toInt()

                for (i in 0..totalSteps) {
                    val currentMs = i * stepMs
                    // OPTION_CLOSEST guarantees exact frame instead of nearest keyframe
                    val bitmap = retriever.getFrameAtTime(currentMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                    
                    if (bitmap != null) {
                        val skeleton = poseAnalyzer.analyzeFrame(bitmap)
                        if (skeleton != null) {
                            skeletons[currentMs] = skeleton
                        }
                    }
                    // Update UI Progress Bar
                    onProgress(i.toFloat() / totalSteps.toFloat())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
        
        val report = swingAnalyzer.analyzeSwing(skeletons)
        Pair(skeletons, report)
    }
}
