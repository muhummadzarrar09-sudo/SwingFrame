package app.swingframe.project

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectCompatibilityTest {
    private fun project(fingerprint: String) = LocalProject(
        sourceUri = "content://source",
        displayName = "swing.mp4",
        durationUs = 1_000_000L,
        width = 1920,
        height = 1080,
        rotationDegrees = 0,
        sourceSizeBytes = 100L,
        sourceLastModifiedEpochMs = 1L,
        sourceFingerprint = fingerprint,
        totalFrames = 60,
        lastFrameIndex = 0,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
    )

    @Test
    fun matchingFingerprint_isAccepted() {
        assertEquals(SourceCompatibility.MATCH, ProjectCompatibility.compare(project("abc"), "abc"))
    }

    @Test
    fun mismatch_isRejectedForConfirmation() {
        assertEquals(SourceCompatibility.MISMATCH, ProjectCompatibility.compare(project("abc"), "xyz"))
    }

    @Test
    fun legacyProject_withoutFingerprint_isUnknownNotRejected() {
        assertEquals(SourceCompatibility.LEGACY_UNKNOWN, ProjectCompatibility.compare(project(""), "xyz"))
    }
}
