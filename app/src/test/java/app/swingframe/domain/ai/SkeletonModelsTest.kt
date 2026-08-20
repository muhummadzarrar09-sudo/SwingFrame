package app.swingframe.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkeletonModelsTest {

    private fun joint(x: Float, y: Float) = PoseJoint(x, y, 0.9f)

    private fun skeleton(shoulderX: Float, hipX: Float) = SwingSkeleton(
        leftShoulder = joint(shoulderX, 0.2f),
        rightShoulder = joint(shoulderX + 0.2f, 0.2f),
        leftElbow = joint(shoulderX, 0.35f),
        rightElbow = joint(shoulderX + 0.2f, 0.35f),
        leftWrist = joint(shoulderX, 0.5f),
        rightWrist = joint(shoulderX + 0.2f, 0.5f),
        leftHip = joint(hipX, 0.6f),
        rightHip = joint(hipX + 0.2f, 0.6f),
        leftKnee = joint(hipX, 0.8f),
        rightKnee = joint(hipX + 0.2f, 0.8f),
        leftAnkle = joint(hipX, 1.0f),
        rightAnkle = joint(hipX + 0.2f, 1.0f)
    )

    @Test
    fun `spine angle is 0 degrees when perfectly vertical`() {
        val s = skeleton(shoulderX = 0.4f, hipX = 0.4f)
        assertEquals(0f, s.getSpineAngleFromVertical()!!, 0.01f)
    }

    @Test
    fun `spine angle is positive when hips shift toward the ball side`() {
        val s = skeleton(shoulderX = 0.4f, hipX = 0.6f)
        val angle = s.getSpineAngleFromVertical()!!
        assert(angle > 0f)
        assertEquals(26.56f, angle, 0.1f)
    }

    @Test
    fun `degenerate skeleton with hips above shoulders is rejected, not wrapped to a huge angle`() {
        // Regression test for the false-CRITICAL failure mode: a detection error placing the
        // hip midpoint above the shoulder midpoint must NOT turn into a ~178-degree change.
        val degenerate = SwingSkeleton(
            leftShoulder = joint(0.4f, 0.6f),
            rightShoulder = joint(0.6f, 0.6f),
            leftElbow = joint(0.4f, 0.5f),
            rightElbow = joint(0.6f, 0.5f),
            leftWrist = joint(0.4f, 0.4f),
            rightWrist = joint(0.6f, 0.4f),
            leftHip = joint(0.4f, 0.2f),
            rightHip = joint(0.6f, 0.2f),
            leftKnee = joint(0.4f, 0.1f),
            rightKnee = joint(0.6f, 0.1f),
            leftAnkle = joint(0.4f, 0.0f),
            rightAnkle = joint(0.6f, 0.0f)
        )
        assertNull(degenerate.getSpineAngleFromVertical())
    }

    @Test
    fun `missing joints yield null midpoints and null spine angle`() {
        val partial = SwingSkeleton(
            leftShoulder = joint(0.4f, 0.2f), rightShoulder = null,
            leftElbow = null, rightElbow = null,
            leftWrist = null, rightWrist = null,
            leftHip = null, rightHip = null,
            leftKnee = null, rightKnee = null,
            leftAnkle = null, rightAnkle = null
        )
        assertNull(partial.midShoulder)
        assertNull(partial.midHip)
        assertNull(partial.getSpineAngleFromVertical())
    }
}
