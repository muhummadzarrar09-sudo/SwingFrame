package app.swingframe.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwingAnalyzerTest {

    private val analyzer = SwingAnalyzer()

    private fun joint(x: Float, y: Float) = PoseJoint(x, y, 0.9f)

    private fun skeleton(leftWristY: Float, rightWristY: Float = leftWristY) = SwingSkeleton(
        leftShoulder = joint(0.4f, 0.2f),
        rightShoulder = joint(0.6f, 0.2f),
        leftElbow = joint(0.4f, 0.35f),
        rightElbow = joint(0.6f, 0.35f),
        leftWrist = joint(0.4f, leftWristY),
        rightWrist = joint(0.6f, rightWristY),
        leftHip = joint(0.4f, 0.6f),
        rightHip = joint(0.6f, 0.6f),
        leftKnee = joint(0.4f, 0.8f),
        rightKnee = joint(0.6f, 0.8f),
        leftAnkle = joint(0.4f, 1.0f),
        rightAnkle = joint(0.6f, 1.0f)
    )

    @Test
    fun `empty timeline is NO_DATA, never a fake score`() {
        val report = analyzer.analyzeSwing(emptyMap())
        assertEquals(AnalysisQuality.NO_DATA, report.quality)
        assertEquals(0, report.score)
        assertTrue(report.flaws.isEmpty())
    }

    @Test
    fun `tiny timeline is INSUFFICIENT_DATA`() {
        val timeline = mapOf(0L to skeleton(0.8f), 100L to skeleton(0.5f))
        val report = analyzer.analyzeSwing(timeline)
        assertEquals(AnalysisQuality.INSUFFICIENT_DATA, report.quality)
    }

    @Test
    fun `realistic swing produces ordered phases and a bounded score`() {
        // Setup (low hands) -> top (high hands) -> impact (low hands again)
        val timeline = linkedMapOf<Long, SwingSkeleton>()
        for (i in 0..9) timeline[i * 100L] = skeleton(leftWristY = 0.8f)   // setup
        for (i in 10..19) timeline[i * 100L] = skeleton(leftWristY = 0.2f) // top of backswing
        for (i in 20..29) timeline[i * 100L] = skeleton(leftWristY = 0.7f) // impact

        val report = analyzer.analyzeSwing(timeline)
        assertEquals(AnalysisQuality.RELIABLE, report.quality)

        val phases = report.phases!!
        assertTrue("setup(${phases.setupMs}) < top(${phases.topOfBackswingMs})", phases.setupMs < phases.topOfBackswingMs)
        assertTrue("top(${phases.topOfBackswingMs}) < impact(${phases.impactMs})", phases.topOfBackswingMs < phases.impactMs)
        assertTrue(report.score in 0..100)
    }

    @Test
    fun `lead wrist is self-calibrating for a left-handed golfer`() {
        // The right wrist sweeps the larger arc (lead hand for a left-handed golfer).
        val timeline = linkedMapOf<Long, SwingSkeleton>()
        for (i in 0..9) timeline[i * 100L] = skeleton(leftWristY = 0.6f, rightWristY = 0.8f)
        for (i in 10..19) timeline[i * 100L] = skeleton(leftWristY = 0.55f, rightWristY = 0.2f)
        for (i in 20..29) timeline[i * 100L] = skeleton(leftWristY = 0.6f, rightWristY = 0.7f)

        val report = analyzer.analyzeSwing(timeline)
        assertEquals(AnalysisQuality.RELIABLE, report.quality)

        val phases = report.phases!!
        assertTrue("setup(${phases.setupMs}) < top(${phases.topOfBackswingMs})", phases.setupMs < phases.topOfBackswingMs)
        assertTrue("top(${phases.topOfBackswingMs}) < impact(${phases.impactMs})", phases.topOfBackswingMs < phases.impactMs)
    }

    @Test
    fun `missing lead wrist in every frame is INSUFFICIENT_DATA`() {
        val timeline = linkedMapOf<Long, SwingSkeleton>()
        for (i in 0..19) timeline[i * 100L] = skeleton(leftWristY = 0.8f, rightWristY = 0.8f).let {
            // Both wrists present here — override to remove them.
            it.copy(leftWrist = null, rightWrist = null)
        }
        val report = analyzer.analyzeSwing(timeline)
        assertEquals(AnalysisQuality.INSUFFICIENT_DATA, report.quality)
    }
}
