package app.swingframe.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import app.swingframe.model.VideoMetadata
import app.swingframe.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class VideoProbe(private val context: Context) {

    suspend fun probe(uri: Uri): VideoSource = withContext(Dispatchers.IO) {
        val documentInfo = queryDocumentInfo(uri)
        val displayName = documentInfo.displayName
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()

        try {
            retriever.setDataSource(context, uri)
            extractor.setDataSource(context, uri, null)

            val videoFormat = findVideoFormat(extractor)
                ?: throw IllegalArgumentException("This file does not contain a readable video track.")

            val durationMs = retriever.metadataLong(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?: videoFormat.longOrNull(MediaFormat.KEY_DURATION)?.div(1_000L)
                ?: throw IllegalArgumentException("The video duration could not be read.")
            val durationUs = durationMs.coerceAtLeast(1L) * 1_000L

            val width = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?: videoFormat.intOrNull(MediaFormat.KEY_WIDTH)
                ?: 0
            val height = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?: videoFormat.intOrNull(MediaFormat.KEY_HEIGHT)
                ?: 0
            val rotation = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: 0
            val mime = videoFormat.getString(MediaFormat.KEY_MIME) ?: "video/unknown"

            val captureRate = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE,
            )?.toFloatOrNull()?.takeIf { it > 0f }
            val formatRate = videoFormat.numberOrNull(MediaFormat.KEY_FRAME_RATE)
                ?.toFloat()?.takeIf { it > 0f }
            val frameCount = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                ?.takeIf { it > 0 }
            val inferredRate = frameCount?.let { count -> count * 1_000_000f / durationUs }
            val declaredFps = captureRate ?: formatRate ?: inferredRate
            val estimatedCount = frameCount ?: declaredFps?.let { fps ->
                (durationUs / 1_000_000f * fps).toInt().coerceAtLeast(1)
            }
            val bitrate = retriever.metadataLong(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?: videoFormat.longOrNull(MediaFormat.KEY_BIT_RATE)
            val fingerprint = sourceFingerprint(
                durationUs = durationUs,
                width = width,
                height = height,
                rotation = rotation,
                mime = mime,
                frameCount = estimatedCount,
                sizeBytes = documentInfo.sizeBytes,
            )

            VideoSource(
                uri = uri,
                displayName = displayName,
                metadata = VideoMetadata(
                    durationUs = durationUs,
                    width = width,
                    height = height,
                    rotationDegrees = rotation,
                    declaredFps = declaredFps,
                    mimeType = mime,
                    codecLabel = codecLabel(mime),
                    estimatedFrameCount = estimatedCount,
                    bitrate = bitrate,
                    sourceSizeBytes = documentInfo.sizeBytes,
                    sourceLastModifiedEpochMs = documentInfo.lastModifiedEpochMs,
                    sourceFingerprint = fingerprint,
                ),
            )
        } finally {
            retriever.release()
            extractor.release()
        }
    }

    private data class DocumentInfo(
        val displayName: String,
        val sizeBytes: Long?,
        val lastModifiedEpochMs: Long?,
    )

    private fun queryDocumentInfo(uri: Uri): DocumentInfo {
        val fallbackName = uri.lastPathSegment ?: "Imported swing"
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                DocumentInfo(
                    displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: fallbackName else fallbackName,
                    sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null,
                    lastModifiedEpochMs = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else null,
                )
            }
        }.getOrNull() ?: DocumentInfo(fallbackName, null, null)
    }

    private fun sourceFingerprint(
        durationUs: Long,
        width: Int,
        height: Int,
        rotation: Int,
        mime: String,
        frameCount: Int?,
        sizeBytes: Long?,
    ): String {
        val identity = listOf(
            durationUs,
            width,
            height,
            rotation,
            mime,
            frameCount ?: -1,
            sizeBytes ?: -1L,
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun findVideoFormat(extractor: MediaExtractor): MediaFormat? {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                return format
            }
        }
        return null
    }

    private fun MediaMetadataRetriever.metadataInt(key: Int): Int? =
        extractMetadata(key)?.toIntOrNull()

    private fun MediaMetadataRetriever.metadataLong(key: Int): Long? =
        extractMetadata(key)?.toLongOrNull()

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun MediaFormat.longOrNull(key: String): Long? =
        if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

    private fun MediaFormat.numberOrNull(key: String): Number? =
        if (containsKey(key)) runCatching { getNumber(key) }.getOrNull() else null

    private fun codecLabel(mime: String): String = when (mime.lowercase()) {
        "video/avc" -> "H.264 / AVC"
        "video/hevc" -> "H.265 / HEVC"
        "video/x-vnd.on2.vp8" -> "VP8"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4 Visual"
        "video/3gpp" -> "H.263"
        else -> mime.substringAfter('/').uppercase()
    }
}
