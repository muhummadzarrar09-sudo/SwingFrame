package app.swingframe.annotation

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** App-private, atomic JSON persistence for lightweight annotation vectors. */
class AnnotationStore(context: Context) {
    private val directory = File(context.filesDir, "annotation-projects").apply { mkdirs() }

    suspend fun load(uri: Uri): Map<Int, List<AnnotationShape>> = withContext(Dispatchers.IO) {
        val file = projectFile(uri)
        if (!file.exists()) return@withContext emptyMap()

        runCatching {
            val text = AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            val frames = root.optJSONObject("frames") ?: return@runCatching emptyMap<Int, List<AnnotationShape>>()
            buildMap<Int, List<AnnotationShape>> {
                val keys = frames.keys()
                while (keys.hasNext()) {
                    val frameIndex = keys.next().toIntOrNull() ?: continue
                    val array = frames.optJSONArray(frameIndex.toString()) ?: continue
                    val shapes = buildList {
                        for (index in 0 until array.length()) {
                            decodeShape(array.optJSONObject(index))?.let(::add)
                        }
                    }
                    if (shapes.isNotEmpty()) put(frameIndex, shapes)
                }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun migrate(oldUri: Uri, newUri: Uri) {
        val existing = load(oldUri)
        if (existing.isNotEmpty()) save(newUri, existing)
        delete(oldUri)
    }

    suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        val file = projectFile(uri)
        AtomicFile(file).delete()
    }

    suspend fun save(uri: Uri, annotations: Map<Int, List<AnnotationShape>>) =
        withContext(Dispatchers.IO) {
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
    }
}
