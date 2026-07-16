package app.swingframe.project

import java.util.UUID

data class RelinkConflict(
    val projectId: String,
    val projectName: String,
    val replacementUri: String,
    val replacementName: String,
    val expectedFingerprint: String,
    val actualFingerprint: String,
)

data class FrameBookmark(
    val id: String = UUID.randomUUID().toString(),
    val frameIndex: Int,
    val label: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class LocalProject(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val displayName: String,
    val durationUs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val sourceSizeBytes: Long?,
    val sourceLastModifiedEpochMs: Long?,
    val sourceFingerprint: String,
    val totalFrames: Int,
    val lastFrameIndex: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val bookmarks: List<FrameBookmark> = emptyList(),
)
