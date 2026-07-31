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

data class SwingReport(
    val score: Int,
    val phases: SwingPhase?,
    val flaws: List<SwingFlaw>
)
