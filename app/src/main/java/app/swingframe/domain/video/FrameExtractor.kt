package app.swingframe.domain.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrameExtractor(private val context: Context) {
    
    /**
     * Extracts an exact frame from the local video file at the specified millisecond.
     */
    suspend fun extractFrame(uri: Uri, positionMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            // getFrameAtTime takes microseconds (ms * 1000)
            // OPTION_CLOSEST guarantees we get the exact frame instead of the nearest keyframe
            retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }
}
