package app.swingframe.annotation

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import app.swingframe.util.LocalDataCorruptionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** App-private, atomic JSON persistence for lightweight annotation vectors. */
class AnnotationStore(context: Context) {
    private val directory = File(context.filesDir, "annotation-projects").apply { mkdirs() }
    private val ioMutex = Mutex()

    suspend fun load(uri: Uri): Map<Int, List<AnnotationShape>> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val file = projectFile(uri)
            if (!file.exists()) return@withLock emptyMap()

            try {
                val text = AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(text)
                val version = root.optInt("version", 1)
                require(version in 1..FORMAT_VERSION) { "Unsupported annotation version $version." }
                val frames = root.optJSONObject("frames") ?: JSONObject()
                buildMap<Int, List<AnnotationShape>> {
                    val keys = frames.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val frameIndex = key.toIntOrNull()
                            ?: error("Invalid annotation frame key: $key")
                        val array = frames.optJSONArray(key)
                            ?: error("Invalid annotation list for frame $frameIndex")
                        val shapes = buildList {
                            for (index in 0 until array.length()) {
                                val shape = decodeShape(array.optJSONObject(index))
                                    ?: error("Invalid annotation at frame $frameIndex, item $index")
                                add(shape)
                            }
                        }
                        if (shapes.isNotEmpty()) put(frameIndex, shapes)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val quarantined = quarantineCorruptFile(file)
                throw LocalDataCorruptionException(
                    "Annotation data was corrupt and was moved to ${quarantined.name}.",
                    error,
                )
            }
        }
    }

    /** Copies first so project metadata can be committed before the old data is removed. */
    suspend fun copy(oldUri: Uri, newUri: Uri) {
        // Write even an empty map so stale orphan data at the destination cannot be inherited.
        save(newUri, load(oldUri))
    }

    suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val file = projectFile(uri)
            AtomicFile(file).delete()
        }
    }

    suspend fun cleanupOrphans(activeUris: Collection<Uri>) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val activeNames = activeUris.mapTo(mutableSetOf()) { projectFile(it).name }
            directory.listFiles()?.forEach { file ->
                val isAnnotationData = file.name.matches(ANNOTATION_FILE_PATTERN)
                if (isAnnotationData && file.name !in activeNames) AtomicFile(file).delete()
            }
        }
    }

    suspend fun save(uri: Uri, annotations: Map<Int, List<AnnotationShape>>) =
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                val root = JSONObject().put("version", FORMAT_VERSION)
                val frames = JSONObject()
                annotations.toSortedMap().forEach { (frameIndex, shapes) ->
                    if (shapes.isNotEmpty()) {
                        frames.put(
                            frameIndex.toString(),
                            JSONArray().apply { shapes.forEach { put(encodeShape(it)) } },
                        )
                    }
                }
                root.put("frames", frames)

                val atomicFile = AtomicFile(projectFile(uri))
                val output = atomicFile.startWrite()
                try {
                    output.write(root.toString().toByteArray(Charsets.UTF_8))
                    output.flush()
                    atomicFile.finishWrite(output)
                } catch (error: Throwable) {
                    atomicFile.failWrite(output)
                    throw error
                }
            }
        }

    private fun quarantineCorruptFile(file: File): File {
        val quarantined = File(
            file.parentFile,
            "${file.nameWithoutExtension}.corrupt-${System.currentTimeMillis()}.json",
        )
        return if (file.renameTo(quarantined)) quarantined else file
    }

    private fun projectFile(uri: Uri): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.json")
    }

    private fun encodeShape(shape: AnnotationShape): JSONObject = JSONObject().apply {
        put("id", shape.id)
        put("style", encodeStyle(shape.style))
        when (shape) {
            is LineAnnotation -> {
                put("type", "line")
                put("start", encodePoint(shape.start))
                put("end", encodePoint(shape.end))
            }
            is AngleAnnotation -> {
                put("type", "angle")
                put("vertex", encodePoint(shape.vertex))
                put("armA", encodePoint(shape.armA))
                put("armB", encodePoint(shape.armB))
            }
            is PlumbAnnotation -> {
                put("type", "plumb")
                put("orientation", shape.orientation.name)
                put("position", shape.position.toDouble())
            }
            is BoxAnnotation -> {
                put("type", "box")
                put("cornerA", encodePoint(shape.cornerA))
                put("cornerB", encodePoint(shape.cornerB))
            }
            is CircleAnnotation -> {
                put("type", "circle")
                put("cornerA", encodePoint(shape.cornerA))
                put("cornerB", encodePoint(shape.cornerB))
            }
            is FreehandAnnotation -> {
                put("type", "freehand")
                put("points", JSONArray().apply { shape.points.forEach { put(encodePoint(it)) } })
            }
        }
    }

    private fun decodeShape(json: JSONObject?): AnnotationShape? {
        json ?: return null
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: newAnnotationId()
        val style = decodeStyle(json.optJSONObject("style"))
        return runCatching {
            when (json.getString("type")) {
                "line" -> LineAnnotation(
                    id = id,
                    start = decodePoint(json.getJSONObject("start")),
                    end = decodePoint(json.getJSONObject("end")),
                    style = style,
                )
                "angle" -> AngleAnnotation(
                    id = id,
                    vertex = decodePoint(json.getJSONObject("vertex")),
                    armA = decodePoint(json.getJSONObject("armA")),
                    armB = decodePoint(json.getJSONObject("armB")),
                    style = style,
                )
                "plumb" -> PlumbAnnotation(
                    id = id,
                    orientation = PlumbOrientation.valueOf(json.getString("orientation")),
                    position = json.getDouble("position").toFloat().coerceIn(0f, 1f),
                    style = style,
                )
                "box" -> BoxAnnotation(
                    id = id,
                    cornerA = decodePoint(json.getJSONObject("cornerA")),
                    cornerB = decodePoint(json.getJSONObject("cornerB")),
                    style = style,
                )
                "circle" -> CircleAnnotation(
                    id = id,
                    cornerA = decodePoint(json.getJSONObject("cornerA")),
                    cornerB = decodePoint(json.getJSONObject("cornerB")),
                    style = style,
                )
                "freehand" -> FreehandAnnotation(
                    id = id,
                    points = json.getJSONArray("points").toPoints(),
                    style = style,
                )
                else -> null
            }
        }.getOrNull()
    }

    private fun encodeStyle(style: AnnotationStyle): JSONObject = JSONObject()
        .put("colorArgb", style.colorArgb)
        .put("strokeWidthDp", style.strokeWidthDp.toDouble())

    private fun decodeStyle(json: JSONObject?): AnnotationStyle = AnnotationStyle(
        colorArgb = json?.optInt("colorArgb", DEFAULT_COLOR) ?: DEFAULT_COLOR,
        strokeWidthDp = json?.optDouble("strokeWidthDp", 3.0)?.toFloat()?.coerceIn(1f, 12f) ?: 3f,
    )

    private fun encodePoint(point: NormalizedPoint): JSONObject = JSONObject()
        .put("x", point.x.toDouble())
        .put("y", point.y.toDouble())

    private fun decodePoint(json: JSONObject): NormalizedPoint = NormalizedPoint(
        x = json.getDouble("x").toFloat(),
        y = json.getDouble("y").toFloat(),
    ).clamped()

    private fun JSONArray.toPoints(): List<NormalizedPoint> = buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(decodePoint(it)) }
        }
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val DEFAULT_COLOR = -1 // White
        val ANNOTATION_FILE_PATTERN = Regex("^[0-9a-f]{64}\\.json$")
    }
}
