package app.swingframe.project

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import app.swingframe.util.LocalDataCorruptionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Atomic app-private index for recent local projects and frame bookmarks. */
class ProjectStore(context: Context) {
    private val indexFile = File(context.filesDir, "projects/index.json").apply {
        parentFile?.mkdirs()
    }
    private val ioMutex = Mutex()

    suspend fun load(): List<LocalProject> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            if (!indexFile.exists()) return@withLock emptyList()
            try {
                val text = AtomicFile(indexFile).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(text)
                val version = root.optInt("version", 1)
                require(version in 1..FORMAT_VERSION) { "Unsupported project index version $version." }
                val array = root.optJSONArray("projects") ?: JSONArray()
                val projects = array.toProjects()
                check(projects.size == array.length()) { "One or more local project records are corrupt." }
                projects
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val quarantined = quarantineCorruptIndex()
                throw LocalDataCorruptionException(
                    "The local project index was corrupt and was moved to ${quarantined.name}.",
                    error,
                )
            }
        }
    }

    suspend fun save(projects: List<LocalProject>) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val root = JSONObject()
                .put("version", FORMAT_VERSION)
                .put(
                    "projects",
                    JSONArray().apply {
                        projects.sortedByDescending { it.updatedAtEpochMs }.forEach { put(encodeProject(it)) }
                    },
                )

            val atomicFile = AtomicFile(indexFile)
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

    private fun encodeProject(project: LocalProject): JSONObject = JSONObject()
        .put("id", project.id)
        .put("sourceUri", project.sourceUri)
        .put("displayName", project.displayName)
        .put("durationUs", project.durationUs)
        .put("width", project.width)
        .put("height", project.height)
        .put("rotationDegrees", project.rotationDegrees)
        .put("sourceSizeBytes", project.sourceSizeBytes)
        .put("sourceLastModifiedEpochMs", project.sourceLastModifiedEpochMs)
        .put("sourceFingerprint", project.sourceFingerprint)
        .put("totalFrames", project.totalFrames)
        .put("lastFrameIndex", project.lastFrameIndex)
        .put("createdAtEpochMs", project.createdAtEpochMs)
        .put("updatedAtEpochMs", project.updatedAtEpochMs)
        .put(
            "bookmarks",
            JSONArray().apply { project.bookmarks.forEach { put(encodeBookmark(it)) } },
        )

    private fun encodeBookmark(bookmark: FrameBookmark): JSONObject = JSONObject()
        .put("id", bookmark.id)
        .put("frameIndex", bookmark.frameIndex)
        .put("label", bookmark.label)
        .put("createdAtEpochMs", bookmark.createdAtEpochMs)

    private fun JSONArray?.toProjects(): List<LocalProject> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                decodeProject(optJSONObject(index))?.let(::add)
            }
        }.sortedByDescending { it.updatedAtEpochMs }
    }

    private fun decodeProject(json: JSONObject?): LocalProject? {
        json ?: return null
        return runCatching {
            val bookmarkArray = json.optJSONArray("bookmarks")
            val bookmarks = bookmarkArray.toBookmarks()
            check(bookmarkArray == null || bookmarks.size == bookmarkArray.length()) {
                "One or more bookmark records are corrupt."
            }
            LocalProject(
                id = json.getString("id"),
                sourceUri = json.getString("sourceUri"),
                displayName = json.getString("displayName"),
                durationUs = json.getLong("durationUs"),
                width = json.getInt("width"),
                height = json.getInt("height"),
                rotationDegrees = json.optInt("rotationDegrees", 0),
                sourceSizeBytes = json.optLong("sourceSizeBytes", -1L).takeIf { it >= 0L },
                sourceLastModifiedEpochMs = json.optLong("sourceLastModifiedEpochMs", -1L).takeIf { it >= 0L },
                sourceFingerprint = json.optString("sourceFingerprint", ""),
                totalFrames = json.getInt("totalFrames"),
                lastFrameIndex = json.optInt("lastFrameIndex", 0),
                createdAtEpochMs = json.getLong("createdAtEpochMs"),
                updatedAtEpochMs = json.getLong("updatedAtEpochMs"),
                bookmarks = bookmarks,
            )
        }.getOrNull()
    }

    private fun JSONArray?.toBookmarks(): List<FrameBookmark> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val json = optJSONObject(index) ?: continue
                runCatching {
                    FrameBookmark(
                        id = json.getString("id"),
                        frameIndex = json.getInt("frameIndex"),
                        label = json.getString("label"),
                        createdAtEpochMs = json.optLong("createdAtEpochMs", 0L),
                    )
                }.getOrNull()?.let(::add)
            }
        }.sortedBy { it.frameIndex }
    }

    private fun quarantineCorruptIndex(): File {
        val quarantined = File(
            indexFile.parentFile,
            "index.corrupt-${System.currentTimeMillis()}.json",
        )
        if (!indexFile.renameTo(quarantined)) {
            // Keep the original in place when the filesystem refuses the rename.
            return indexFile
        }
        return quarantined
    }

    private companion object {
        const val FORMAT_VERSION = 2
    }
}
