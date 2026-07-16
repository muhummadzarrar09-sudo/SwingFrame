package app.swingframe.project

enum class SourceCompatibility { MATCH, LEGACY_UNKNOWN, MISMATCH }

object ProjectCompatibility {
    fun compare(project: LocalProject, replacementFingerprint: String): SourceCompatibility = when {
        project.sourceFingerprint.isBlank() -> SourceCompatibility.LEGACY_UNKNOWN
        project.sourceFingerprint == replacementFingerprint -> SourceCompatibility.MATCH
        else -> SourceCompatibility.MISMATCH
    }
}
