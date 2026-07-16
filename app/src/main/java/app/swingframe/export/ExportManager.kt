package app.swingframe.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import app.swingframe.annotation.AnnotationShape
import app.swingframe.data.Media3FrameDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@androidx.annotation.OptIn(UnstableApi::class)
class ExportManager(private val context: Context) {

    suspend fun exportStill(
        sourceUri: Uri,
        timestampUs: Long,
        annotations: List<AnnotationShape>,
        carriedAnnotations: List<AnnotationShape>,
        burnAnnotations: Boolean,
    ): Uri {
        val decoder = Media3FrameDecoder(context, sourceUri)
        val sourceBitmap = try {
            decoder.decodeSourceFrame(timestampUs)
        } finally {
            decoder.close()
        }
        val outputBitmap = if (burnAnnotations && (annotations.isNotEmpty() || carriedAnnotations.isNotEmpty())) {
            AnnotationBitmapRenderer.renderCopy(sourceBitmap, annotations, carriedAnnotations)
        } else {
            sourceBitmap
        }

        return try {
            withContext(Dispatchers.IO) { saveImageToMediaStore(outputBitmap) }
        } finally {
            if (outputBitmap !== sourceBitmap && !outputBitmap.isRecycled) outputBitmap.recycle()
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }

    @Suppress("DEPRECATION")
    suspend fun exportVideo(
        request: VideoExportRequest,
        onProgress: (Int) -> Unit,
    ): Uri = coroutineScope {
        val safeStart = request.startFrameIndex.coerceIn(request.frameTimestampsUs.indices)
        val safeEnd = request.endFrameIndex.coerceIn(safeStart, request.frameTimestampsUs.lastIndex)
        val sourceStartUs = request.frameTimestampsUs[safeStart]
        val sourceEndExclusiveUs = ExportTimelineMapper.endExclusiveTimestampUs(request.frameTimestampsUs, safeEnd)
        val speed = request.playbackSpeed.coerceIn(0.1f, 1f)
        val tempDirectory = File(context.cacheDir, "exports").apply { mkdirs() }
        val tempFile = File(tempDirectory, "${UUID.randomUUID()}.mp4")

        suspendCancellableCoroutine { continuation ->
            var progressJob: Job? = null
            lateinit var transformer: Transformer

            val videoEffects = mutableListOf<Effect>()
            if (speed != 1f) videoEffects += SpeedChangeEffect(speed)
            if (request.burnAnnotations) {
                val overlay = object : CanvasOverlay(/* useInputFrameSize= */ true) {
                    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        val sourceTimeUs = ExportTimelineMapper.sourceTimeUs(sourceStartUs, presentationTimeUs, speed)
                        val frameIndex = ExportTimelineMapper.nearestFrameIndex(request.frameTimestampsUs, sourceTimeUs)
                            .coerceIn(safeStart, safeEnd)
                        val exact = request.annotationsByFrame[frameIndex].orEmpty()
                        val carried = if (request.carryForward && exact.isEmpty()) {
                            carriedAnnotations(request.annotationsByFrame, frameIndex)
                        } else {
                            emptyList()
                        }
                        AnnotationBitmapRenderer.draw(canvas, canvas.width, canvas.height, carried, alpha = 0.34f)
                        AnnotationBitmapRenderer.draw(canvas, canvas.width, canvas.height, exact)
                    }
                }
                videoEffects += OverlayEffect(listOf(overlay))
            }

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(sourceStartUs / 1_000L)
                .setEndPositionMs(sourceEndExclusiveUs / 1_000L)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(request.sourceUri)
                .setClippingConfiguration(clipping)
                .build()
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(speed != 1f)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    progressJob?.cancel()
                    this@coroutineScope.launch {
                        try {
                            val uri = withContext(Dispatchers.IO) { copyVideoToMediaStore(tempFile) }
                            tempFile.delete()
                            if (continuation.isActive) continuation.resumeWith(Result.success(uri))
                        } catch (error: Throwable) {
                            tempFile.delete()
                            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    progressJob?.cancel()
                    tempFile.delete()
                    if (continuation.isActive) continuation.resumeWith(Result.failure(exportException))
                }
            }

            transformer = Transformer.Builder(context)
                .setUsePlatformDiagnostics(false)
                .addListener(listener)
                .build()

            progressJob = this@coroutineScope.launch {
                val holder = ProgressHolder()
                while (isActive) {
                    val state = transformer.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress.coerceIn(0, 99))
                    }
                    delay(PROGRESS_POLL_MS)
                }
            }

            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post {
                    transformer.cancel()
                    progressJob?.cancel()
                    tempFile.delete()
                }
            }

            try {
                transformer.start(editedMediaItem, tempFile.absolutePath)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                progressJob?.cancel()
                tempFile.delete()
                if (continuation.isActive) continuation.resumeWith(Result.failure(error))
            }
        }
    }

    private fun saveImageToMediaStore(bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "SwingFrame_${timestamp()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SwingFrame")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Android could not create the image in MediaStore.")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "The frame image could not be encoded."
                }
            } ?: error("Android could not open the image output stream.")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun copyVideoToMediaStore(tempFile: File): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "SwingFrame_${timestamp()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SwingFrame")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Android could not create the video in MediaStore.")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                tempFile.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: error("Android could not open the video output stream.")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun carriedAnnotations(
        annotations: Map<Int, List<AnnotationShape>>,
        frameIndex: Int,
    ): List<AnnotationShape> {
        for (distance in 1..3) {
            annotations[frameIndex - distance]?.takeIf { it.isNotEmpty() }?.let { return it }
            annotations[frameIndex + distance]?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private companion object {
        const val PROGRESS_POLL_MS = 180L
    }
}
