package app.swingframe.domain.ai

import kotlin.math.atan2
import kotlin.math.toDegrees

data class PoseJoint(
    val x: Float,
    val y: Float,
    val confidence: Float
)

data class SwingSkeleton(
    val leftShoulder: PoseJoint?,
    val rightShoulder: PoseJoint?,
    val leftElbow: PoseJoint?,
    val rightElbow: PoseJoint?,
    val leftWrist: PoseJoint?,
    val rightWrist: PoseJoint?,
    val leftHip: PoseJoint?,
    val rightHip: PoseJoint?,
    val leftKnee: PoseJoint?,
    val rightKnee: PoseJoint?,
    val leftAnkle: PoseJoint?,
    val rightAnkle: PoseJoint?
) {
    /** Midpoint of the shoulders, or null if either shoulder is missing. */
    val midShoulder: PoseJoint?
        get() = midpoint(leftShoulder, rightShoulder)

    /** Midpoint of the hips, or null if either hip is missing. */
    val midHip: PoseJoint?
        get() = midpoint(leftHip, rightHip)

    /**
     * Spine angle in degrees measured FROM THE VERTICAL: 0 = perfectly upright spine,
     * positive = leaning toward +X (the ball side in a down-the-line view).
     *
     * Uses atan2(deltaX, deltaY) rather than atan2(deltaY, deltaX). The latter measures
     * from the horizontal axis and reports ~90 degrees for an upright spine, which makes
     * the number meaningless on its own; worse, if a detection error ever places the hip
     * midpoint ABOVE the shoulder midpoint, atan2(deltaY, deltaX) jumps ~180 degrees and
     * a tiny posture change turns into a false CRITICAL "Early Extension". The vertical
     * reference is continuous across that boundary, and degenerate skeletons (hips above
     * shoulders) are rejected outright as unmeasurable.
     */
    fun getSpineAngleFromVertical(): Float? {
        val shoulder = midShoulder ?: return null
        val hip = midHip ?: return null
        val deltaY = hip.y - shoulder.y
        if (deltaY <= 0f) return null // degenerate/inverted spine — cannot be measured
        val deltaX = hip.x - shoulder.x
        return atan2(deltaX, deltaY).toDouble().toDegrees().toFloat()
    }

    private fun midpoint(a: PoseJoint?, b: PoseJoint?): PoseJoint? {
        if (a == null || b == null) return null
        return PoseJoint(
            x = (a.x + b.x) / 2f,
            y = (a.y + b.y) / 2f,
            confidence = (a.confidence + b.confidence) / 2f
        )
    }
}
