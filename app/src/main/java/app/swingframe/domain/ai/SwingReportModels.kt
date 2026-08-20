package app.swingframe.domain.ai

data class SwingPhase(
    val setupMs: Long,
    val topOfBackswingMs: Long,
    val impactMs: Long
)

data class SwingFlaw(
    val name: String,
    val description: String,
    val severity: FlawSeverity // WARNING, CRITICAL, INFO
)

enum class FlawSeverity { INFO, WARNING, CRITICAL }

/**
 * How much trust the diagnostics deserve, based on how much usable pose data was captured.
 * The UI must never present a score when the data cannot support one.
 */
enum class AnalysisQuality {
    /** Enough skeletons over a long enough span for the heuristics to mean something. */
    RELIABLE,

    /** Some poses were seen but not enough to locate phases — show a warning, not a score. */
    INSUFFICIENT_DATA,

    /** No usable pose data at all — the video was never meaningfully analyzed. */
    NO_DATA
}

data class SwingReport(
    val score: Int,
    val phases: SwingPhase?,
    val flaws: List<SwingFlaw>,
    val quality: AnalysisQuality = AnalysisQuality.RELIABLE,
    val message: String? = null
)

/** Outcome of the full extract-and-analyze pipeline; distinguishes I/O failure from analysis output. */
sealed interface SwingAnalysisResult {
    data class Success(
        val skeletons: Map<Long, SwingSkeleton>,
        val report: SwingReport
    ) : SwingAnalysisResult

    data class Failure(val message: String) : SwingAnalysisResult
}
