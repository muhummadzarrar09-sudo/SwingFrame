package app.swingframe.annotation

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AnnotationFrameSnapshot(
    val current: List<AnnotationShape>,
    val carried: List<AnnotationShape>,
    val canUndo: Boolean,
    val canRedo: Boolean,
)

/** Owns annotation data, history, carry-forward resolution, and persistence. */
class AnnotationSession(
    private val store: AnnotationStore,
    private val scope: CoroutineScope,
    private val onPersistenceError: (Throwable) -> Unit = {},
) {
    private val byFrame = mutableMapOf<Int, MutableList<AnnotationShape>>()
    private val undo = mutableMapOf<Int, ArrayDeque<List<AnnotationShape>>>()
    private val redo = mutableMapOf<Int, ArrayDeque<List<AnnotationShape>>>()
    private var sourceUri: Uri? = null
    private var saveJob: Job? = null

    suspend fun open(uri: Uri) {
        flush()
        sourceUri = uri
        byFrame.clear()
        store.load(uri).forEach { (frame, shapes) -> byFrame[frame] = shapes.toMutableList() }
        undo.clear()
        redo.clear()
    }

    fun snapshot(frameIndex: Int, carryForward: Boolean): AnnotationFrameSnapshot =
        AnnotationFrameSnapshot(
            current = byFrame[frameIndex]?.toList().orEmpty(),
            carried = if (carryForward) carriedFor(frameIndex) else emptyList(),
            canUndo = undo[frameIndex]?.isNotEmpty() == true,
            canRedo = redo[frameIndex]?.isNotEmpty() == true,
        )

    fun add(frameIndex: Int, shape: AnnotationShape): AnnotationFrameSnapshot =
        mutate(frameIndex) { it + shape }

    fun replace(frameIndex: Int, shape: AnnotationShape): AnnotationFrameSnapshot =
        mutate(frameIndex) { list -> list.map { if (it.id == shape.id) shape else it } }

    fun updateStyle(
        frameIndex: Int,
        shapeId: String,
        transform: (AnnotationStyle) -> AnnotationStyle,
    ): AnnotationFrameSnapshot = mutate(frameIndex) { list ->
        list.map { if (it.id == shapeId) it.withStyle(transform(it.style)) else it }
    }

    fun delete(frameIndex: Int, shapeId: String): AnnotationFrameSnapshot =
        mutate(frameIndex) { it.filterNot { shape -> shape.id == shapeId } }

    fun clear(frameIndex: Int): AnnotationFrameSnapshot = mutate(frameIndex) { emptyList() }

    fun undo(frameIndex: Int): AnnotationFrameSnapshot {
        val stack = undo[frameIndex]
        if (stack.isNullOrEmpty()) return snapshot(frameIndex, false)
        redo.getOrPut(frameIndex) { ArrayDeque() }.addLast(byFrame[frameIndex]?.toList().orEmpty())
        set(frameIndex, stack.removeLast())
        scheduleSave()
        return snapshot(frameIndex, false)
    }

    fun redo(frameIndex: Int): AnnotationFrameSnapshot {
        val stack = redo[frameIndex]
        if (stack.isNullOrEmpty()) return snapshot(frameIndex, false)
        pushUndo(frameIndex, byFrame[frameIndex]?.toList().orEmpty())
        set(frameIndex, stack.removeLast())
        scheduleSave()
        return snapshot(frameIndex, false)
    }

    fun allFrames(): Map<Int, List<AnnotationShape>> = byFrame.mapValues { it.value.toList() }

    fun hasAnnotations(frameIndex: Int): Boolean = byFrame[frameIndex].isNullOrEmpty().not()

    fun close() {
        flush()
        sourceUri = null
        byFrame.clear()
        undo.clear()
        redo.clear()
    }

    private fun mutate(
        frameIndex: Int,
        transform: (List<AnnotationShape>) -> List<AnnotationShape>,
    ): AnnotationFrameSnapshot {
        val before = byFrame[frameIndex]?.toList().orEmpty()
        val after = transform(before)
        if (after == before) return snapshot(frameIndex, false)
        pushUndo(frameIndex, before)
        redo[frameIndex]?.clear()
        set(frameIndex, after)
        scheduleSave()
        return snapshot(frameIndex, false)
    }

    private fun set(frameIndex: Int, shapes: List<AnnotationShape>) {
        if (shapes.isEmpty()) byFrame.remove(frameIndex)
        else byFrame[frameIndex] = shapes.toMutableList()
    }

    private fun pushUndo(frameIndex: Int, snapshot: List<AnnotationShape>) {
        if (frameIndex !in undo && undo.size >= MAX_HISTORY_FRAMES) {
            val oldestFrame = undo.keys.firstOrNull()
            if (oldestFrame != null) {
                undo.remove(oldestFrame)
                redo.remove(oldestFrame)
            }
        }
        val stack = undo.getOrPut(frameIndex) { ArrayDeque() }
        if (stack.size >= HISTORY_LIMIT) stack.removeFirst()
        stack.addLast(snapshot)
    }

    private fun carriedFor(frameIndex: Int): List<AnnotationShape> {
        for (distance in 1..CARRY_RADIUS) {
            byFrame[frameIndex - distance]?.takeIf { it.isNotEmpty() }?.let { return it.toList() }
            byFrame[frameIndex + distance]?.takeIf { it.isNotEmpty() }?.let { return it.toList() }
        }
        return emptyList()
    }

    private fun scheduleSave() {
        val uri = sourceUri ?: return
        val data = allFrames()
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            try {
                store.save(uri, data)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onPersistenceError(error)
            }
        }
    }

    private fun flush() {
        val uri = sourceUri ?: return
        val data = allFrames()
        saveJob?.cancel()
        saveJob = null
        scope.launch {
            try {
                store.save(uri, data)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onPersistenceError(error)
            }
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 50
        const val MAX_HISTORY_FRAMES = 24
        const val CARRY_RADIUS = 3
        const val SAVE_DEBOUNCE_MS = 120L
    }
}
