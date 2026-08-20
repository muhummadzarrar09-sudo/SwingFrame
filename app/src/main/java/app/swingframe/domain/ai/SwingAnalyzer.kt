package app.swingframe.domain.ai

import java.util.Locale
import kotlin.math.abs

/**
 * Turns a pose timeline into swing phases and a flaw diagnosis using conservative
 * biomechanical heuristics.
 *
 * Handedness is self-calibrating: the lead hand sweeps the largest vertical arc in a
 * down-the-line view, so the wrist with the biggest Y-range is used for phase detection.
 * This works for left- and right-handed golfers without a settings toggle, and never
 * falls back to bogus 0f/1f defaults for missing joints.
 *
 * The analyzer never invents a score: if there is not enough usable data it returns a
 * report whose [AnalysisQuality] is not [AnalysisQuality.RELIABLE], and the UI shows
 * [SwingReport.message] instead of a number.
 */
class SwingAnalyzer(
    /** Minimum number of frames with a detected pose before heuristics are allowed to run. */
    private val minFramesForAnalysis: Int = 8,
    /** Minimum time span covered by the detected frames (a real swing takes > 400 ms). */
    private val minSpanMsForAnalysis: Long = 400L
) {

    fun analyzeSwing(timeline: Map<Long, SwingSkeleton>): SwingReport {
        if (timeline.isEmpty()) {
            return SwingReport(
                score = 0,
                phases = null,
                flaws = emptyList(),
                quality = AnalysisQuality.NO_DATA,
                message = "No pose data was captured. Make sure the golfer is fully visible, well lit, and facing the camera."
            )
        }

        val times = timeline.keys.sorted()
        val spanMs = times.last() - times.first()
        if (timeline.size < minFramesForAnalysis || spanMs < minSpanMsForAnalysis) {
            return SwingReport(
                score = 0,
                phases = null,
                flaws = emptyList(),
                quality = AnalysisQuality.INSUFFICIENT_DATA,
                message = "Only ${timeline.size} pose frames over ${spanMs} ms were captured — too few for a reliable diagnosis. Move the camera closer or improve lighting."
            )
        }

        // ---- 1. Self-calibrating lead wrist ---------------------------------
        val leftRange = verticalRange(timeline) { it.leftWrist }
        val rightRange = verticalRange(timeline) { it.rightWrist }
        val leadWristY: (SwingSkeleton) -> Float? =
            if (rightRange > leftRange) { s -> s.rightWrist?.y } else { s -> s.leftWrist?.y }

        val usable = timeline.entries.filter { leadWristY(it.value) != null }
        if (usable.isEmpty()) {
            return SwingReport(
                score = 0,
                phases = null,
                flaws = emptyList(),
                quality = AnalysisQuality.INSUFFICIENT_DATA,
                message = "The golfer's hands were not detected in any frame, so swing phases could not be identified."
            )
        }

        // ---- 2. Key phases from the lead wrist -------------------------------
        // In Android coordinates Y grows downward: lower Y = higher hands.
        // Setup = lowest hands early in the timeline; top = highest hands after setup;
        // impact = lowest hands after the top.
        val setupEntry = usable.take(usable.size / 3).maxByOrNull { leadWristY(it.value)!! }
        val setupTime = setupEntry?.key ?: times.first()

        val afterSetup = usable.filter { it.key > setupTime }
        val topEntry = afterSetup.minByOrNull { leadWristY(it.value)!! }
        val topTime = topEntry?.key ?: setupTime

        val afterTop = usable.filter { it.key > topTime }
        val impactEntry = afterTop.maxByOrNull { leadWristY(it.value)!! }
        val impactTime = impactEntry?.key ?: topTime

        val phases = SwingPhase(setupTime, topTime, impactTime)

        // ---- 3. Diagnose flaws ------------------------------------------------
        val flaws = mutableListOf<SwingFlaw>()
        var score = 100

        val setupSkeleton = setupEntry?.value
        val topSkeleton = topEntry?.value
        val impactSkeleton = impactEntry?.value

        if (setupSkeleton != null && topSkeleton != null && impactSkeleton != null) {

            // FLAW A: Early Extension (spine angle at setup vs impact)
            val setupSpine = setupSkeleton.getSpineAngleFromVertical()
            val impactSpine = impactSkeleton.getSpineAngleFromVertical()
            if (setupSpine != null && impactSpine != null) {
                val spineDiff = abs(setupSpine - impactSpine)
                if (spineDiff > 12f) { // More than 12 degrees of change is a critical flaw
                    flaws.add(
                        SwingFlaw(
                            name = "Early Extension",
                            description = "Your spine angle changed by ${spineDiff.toInt()}° at impact. You are standing up and thrusting your hips toward the ball.",
                            severity = FlawSeverity.CRITICAL
                        )
                    )
                    score -= 20
                } else if (spineDiff > 5f) {
                    flaws.add(
                        SwingFlaw(
                            name = "Slight Posture Loss",
                            description = "Your spine angle changed by ${spineDiff.toInt()}°. Try to stay in your posture through the ball.",
                            severity = FlawSeverity.WARNING
                        )
                    )
                    score -= 5
                }
            }

            // FLAW B: Swaying (shoulder midpoint moves too far laterally on the backswing)
            val setupMidShoulderX = setupSkeleton.midShoulder?.x
            val topMidShoulderX = topSkeleton.midShoulder?.x
            if (setupMidShoulderX != null && topMidShoulderX != null) {
                val lateralShift = abs(setupMidShoulderX - topMidShoulderX)
                if (lateralShift > 0.1f) { // Moved more than 10% of the frame width
                    flaws.add(
                        SwingFlaw(
                            name = "Backswing Sway",
                            description = "Your upper body shifted laterally off the ball. Rotate around your spine instead of sliding.",
                            severity = FlawSeverity.WARNING
                        )
                    )
                    score -= 10
                }
            }

            // FLAW C: Tempo (ratio of backswing to downswing time; pro ideal is ~3:1)
            val backswingMs = topTime - setupTime
            val downswingMs = impactTime - topTime
            if (backswingMs > 0 && downswingMs > 0) {
                val tempo = backswingMs.toFloat() / downswingMs.toFloat()
                if (tempo < 2.0f) {
                    flaws.add(
                        SwingFlaw(
                            name = "Quick Transition",
                            description = "Your tempo is ${formatTempo(tempo)}:1. Pros average 3:1. Your backswing is too fast relative to your downswing.",
                            severity = FlawSeverity.INFO
                        )
                    )
                    score -= 5
                } else if (tempo > 4.0f) {
                    flaws.add(
                        SwingFlaw(
                            name = "Slow Backswing",
                            description = "Your tempo is ${formatTempo(tempo)}:1. You might be decelerating into impact.",
                            severity = FlawSeverity.INFO
                        )
                    )
                    score -= 5
                }
            }
        }

        if (score < 0) score = 0
        if (flaws.isEmpty()) {
            flaws.add(
                SwingFlaw(
                    name = "No Major Flaws Detected",
                    description = "Based on the captured data, posture and sequence stayed within normal ranges. Film a few more swings for a fuller picture.",
                    severity = FlawSeverity.INFO
                )
            )
        }

        return SwingReport(score, phases, flaws, AnalysisQuality.RELIABLE)
    }

    /** Vertical travel (maxY - minY) of a wrist over the whole timeline; 0 if never detected. */
    private fun verticalRange(
        timeline: Map<Long, SwingSkeleton>,
        wrist: (SwingSkeleton) -> PoseJoint?
    ): Float {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var found = false
        for (skeleton in timeline.values) {
            val y = wrist(skeleton)?.y ?: continue
            if (y < min) min = y
            if (y > max) max = y
            found = true
        }
        return if (found) max - min else 0f
    }

    private fun formatTempo(tempo: Float): String =
        String.format(Locale.US, "%.1f", tempo)
}
