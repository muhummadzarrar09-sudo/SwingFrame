package app.swingframe.media

import android.content.Context
import android.graphics.Bitmap
import app.swingframe.data.BitmapFrameCache
import app.swingframe.data.Media3FrameDecoder
import app.swingframe.data.TimelineThumbnailLoader
import app.swingframe.model.IndexedVideo
import app.swingframe.model.TimelineThumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong

/** Resolved pixels are always explicitly coupled to [resolvedFrameIndex]. */
data class MediaSessionState(
    val video: IndexedVideo? = null,
    val requestedFrameIndex: Int = 0,
    val resolvedFrameIndex: Int? = null,
    val bitmap: Bitmap? = null,
    val thumbnails: List<TimelineThumbnail> = emptyList(),
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
)

class MediaSessionController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val thumbnailLoader = TimelineThumbnailLoader(appContext)
    private val cache = BitmapFrameCache(appContext)
    private val requests = Channel<Int>(Channel.CONFLATED)
    private val _state = MutableStateFlow(MediaSessionState())
    val state: StateFlow<MediaSessionState> = _state.asStateFlow()

    private var decoder: Media3FrameDecoder? = null
    private var workerJob: Job? = null
    private var thumbnailJob: Job? = null
    private var playbackJob: Job? = null
    private var previousRequested = 0
    private var suspendedForExport = false

    fun start(video: IndexedVideo, initialFrameIndex: Int) {
        release(clearState = false)
        val initial = initialFrameIndex.coerceIn(0, video.totalFrames - 1)
        previousRequested = initial
        decoder = Media3FrameDecoder(appContext, video.source.uri)
        _state.value = MediaSessionState(
            video = video,
            requestedFrameIndex = initial,
            isLoading = true,
        )
        startWorker(video)
        request(initial, stopPlayback = false)
        startThumbnails(video)
    }

    fun request(frameIndex: Int, stopPlayback: Boolean = true) {
        val video = _state.value.video ?: return
        val target = frameIndex.coerceIn(0, video.totalFrames - 1)
        if (stopPlayback) stopPlayback()
        val cached = cache[target]
        _state.update {
            it.copy(
                requestedFrameIndex = target,
                resolvedFrameIndex = if (cached != null) target else null,
                bitmap = cached,
                isLoading = cached == null,
                errorMessage = null,
            )
        }
        if (!suspendedForExport) requests.trySend(target)
    }

    fun step(delta: Int) {
        request(_state.value.requestedFrameIndex + delta)
    }

    fun togglePlayback(speed: Float) {
        if (_state.value.isPlaying) stopPlayback() else startPlayback(speed)
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _state.update { it.copy(isPlaying = false) }
    }

    fun pauseForExport() {
        suspendedForExport = true
        stopPlayback()
        workerJob?.cancel()
        workerJob = null
        drainRequests()
        decoder?.close()
        decoder = null
    }

    fun resumeAfterExport() {
        val video = _state.value.video ?: return
        if (!suspendedForExport) return
        suspendedForExport = false
        decoder = Media3FrameDecoder(appContext, video.source.uri)
        startWorker(video)
        request(_state.value.requestedFrameIndex, stopPlayback = false)
    }

    fun release(clearState: Boolean = true) {
        suspendedForExport = false
        stopPlayback()
        workerJob?.cancel()
        workerJob = null
        thumbnailJob?.cancel()
        thumbnailJob = null
        drainRequests()
        decoder?.close()
        decoder = null
        cache.clear()
        if (clearState) _state.value = MediaSessionState()
    }

    private fun startWorker(video: IndexedVideo) {
        workerJob?.cancel()
        drainRequests()
        workerJob = scope.launch {
            requestLoop@ while (isActive) {
                var requested = requests.receive()
                while (true) requested = requests.tryReceive().getOrNull() ?: break
                val direction = when {
                    requested > previousRequested -> 1
                    requested < previousRequested -> -1
                    else -> 1
                }
                previousRequested = requested

                val exact = load(video, requested, reportFailure = true)
                if (exact != null && _state.value.requestedFrameIndex == requested) {
                    _state.update {
                        it.copy(
                            requestedFrameIndex = requested,
                            resolvedFrameIndex = requested,
                            bitmap = exact,
                            isLoading = false,
                        )
                    }
                }

                val neighbors = buildList {
                    for (distance in 1..PREFETCH_RADIUS) {
                        add(requested + direction * distance)
                        add(requested - direction * distance)
                    }
                }
                for (neighbor in neighbors) {
                    val pending = requests.tryReceive().getOrNull()
                    if (pending != null || _state.value.requestedFrameIndex != requested) {
                        if (pending != null) requests.trySend(pending)
                        continue@requestLoop
                    }
                    if (neighbor in 0 until video.totalFrames && cache[neighbor] == null) {
                        load(video, neighbor, reportFailure = false)
                    }
                }
            }
        }
    }

    private suspend fun load(video: IndexedVideo, frameIndex: Int, reportFailure: Boolean): Bitmap? {
        cache[frameIndex]?.let { return it }
        return try {
            val bitmap = decoder?.decodePreviewFrame(video.frameTimestampsUs[frameIndex])
                ?: error("Frame decoder is unavailable.")
            cache.put(frameIndex, bitmap)
            bitmap
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (reportFailure && _state.value.requestedFrameIndex == frameIndex) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = false,
                        errorMessage = error.message ?: "Frame ${frameIndex + 1} could not be decoded.",
                    )
                }
            }
            null
        }
    }

    private fun startThumbnails(video: IndexedVideo) {
        thumbnailJob?.cancel()
        thumbnailJob = scope.launch {
            delay(350L)
            try {
                thumbnailLoader.load(video) { thumbnail ->
                    _state.update { current ->
                        val merged = (current.thumbnails + thumbnail)
                            .associateBy { it.frameIndex }
                            .values
                            .sortedBy { it.frameIndex }
                        current.copy(thumbnails = merged)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Thumbnail orientation is optional; exact frame decode remains active.
            }
        }
    }

    private fun startPlayback(speed: Float) {
        val video = _state.value.video ?: return
        if (video.totalFrames < 2) return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            var index = _state.value.requestedFrameIndex
            if (index >= video.totalFrames - 1) {
                index = 0
                request(index, stopPlayback = false)
            }
            _state.update { it.copy(isPlaying = true) }
            while (isActive && index < video.totalFrames - 1) {
                val next = index + 1
                val deltaUs = (video.frameTimestampsUs[next] - video.frameTimestampsUs[index])
                    .coerceAtLeast(1_000L)
                delay((deltaUs / 1_000f / speed.coerceIn(0.1f, 1f)).roundToLong().coerceAtLeast(4L))
                index = next
                request(index, stopPlayback = false)
                val resolved = withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                    while (isActive && cache[index] == null && _state.value.errorMessage == null) delay(4L)
                    cache[index] != null
                } ?: false
                if (!resolved) break
            }
            _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun drainRequests() {
        while (requests.tryReceive().isSuccess) Unit
    }

    private companion object {
        const val PREFETCH_RADIUS = 2
        const val FRAME_TIMEOUT_MS = 5_000L
    }
}
