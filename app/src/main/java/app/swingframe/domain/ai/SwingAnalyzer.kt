package app.swingframe.domain.ai

import kotlin.math.abs

class SwingAnalyzer {

    /**
     * Takes the full timeline of skeletons and uses biomechanical heuristics
     * to find the key swing checkpoints and diagnose flaws.
     */
    fun analyzeSwing(timeline: Map<Long, SwingSkeleton>): SwingReport {
        if (timeline.isEmpty()) return SwingReport(0, null, emptyList())

        // 1. Identify Key Phases based on Wrist Position (Heuristic Approach)
        // In Android coordinates, Y=0 is the top of the screen. 
        // Higher Y = lower to the ground.
        
        // Find Setup (Lowest hands early in the timeline)
        val setupEntry = timeline.entries.take(timeline.size / 3)
            .maxByOrNull { it.value.leftWrist?.y ?: 0f } 
        
        // Find Top of Backswing (Highest hands / lowest Y value after setup)
        val setupTime = setupEntry?.key ?: 0L
        val topEntry = timeline.entries.filter { it.key > setupTime }
            .minByOrNull { it.value.leftWrist?.y ?: 1f }
            
        // Find Impact (Hands return to lowest point after the top)
        val topTime = topEntry?.key ?: setupTime
        val impactEntry = timeline.entries.filter { it.key > topTime }
            .maxByOrNull { it.value.leftWrist?.y ?: 0f }

        val impactTime = impactEntry?.key ?: topTime

        val phases = SwingPhase(setupTime, topTime, impactTime)
        
        // 2. Diagnose Flaws
        val flaws = mutableListOf<SwingFlaw>()
        var score = 100

        val setupSkeleton = setupEntry?.value
        val topSkeleton = topEntry?.value
        val impactSkeleton = impactEntry?.value

        if (setupSkeleton != null && topSkeleton != null && impactSkeleton != null) {
            
            // FLAW A: Early Extension (Hips move toward the ball, losing spine angle)
            val setupSpine = setupSkeleton.getSpineAngle()
            val impactSpine = impactSkeleton.getSpineAngle()
            
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

            // FLAW B: Swaying (Head/Shoulder center moves too far laterally on backswing)
            val setupMidShoulderX = ((setupSkeleton.leftShoulder?.x ?: 0f) + (setupSkeleton.rightShoulder?.x ?: 0f)) / 2
            val topMidShoulderX = ((topSkeleton.leftShoulder?.x ?: 0f) + (topSkeleton.rightShoulder?.x ?: 0f)) / 2
            
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
            
            // FLAW C: Tempo (Ratio of Backswing time to Downswing time. Pro ideal is 3:1)
            val backswingTime = topTime - setupTime
            val downswingTime = impactTime - topTime
            if (downswingTime > 0) {
                val tempo = backswingTime.toFloat() / downswingTime.toFloat()
                if (tempo < 2.0f) {
                    flaws.add(
                        SwingFlaw(
                            name = "Quick Transition",
                            description = "Your tempo is ${String.format("%.1f", tempo)}:1. Pros average 3:1. Your backswing is too fast relative to your downswing.",
                            severity = FlawSeverity.INFO
                        )
                    )
                    score -= 5
                } else if (tempo > 4.0f) {
                    flaws.add(
                        SwingFlaw(
                            name = "Slow Backswing",
                            description = "Your tempo is ${String.format("%.1f", tempo)}:1. You might be decelerating into impact.",
                            severity = FlawSeverity.INFO
                        )
                    )
                    score -= 5
                }
            }
        }

        // Perfect score floor
        if (score < 0) score = 0
        if (flaws.isEmpty()) {
            flaws.add(
                SwingFlaw(
                    name = "Tour Level Biomechanics",
                    description = "Your posture and sequence are incredibly solid. Keep it up.",
                    severity = FlawSeverity.INFO
                )
            )
        }

        return SwingReport(score, phases, flaws)
    }
}
