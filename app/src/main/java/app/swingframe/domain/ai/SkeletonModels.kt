package app.swingframe.domain.ai

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
    fun getSpineAngle(): Float? {
        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) return null
        
        // Midpoint of shoulders
        val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2
        val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2
        
        // Midpoint of hips
        val midHipX = (leftHip.x + rightHip.x) / 2
        val midHipY = (leftHip.y + rightHip.y) / 2
        
        // Calculate angle relative to vertical
        val deltaY = midHipY - midShoulderY
        val deltaX = midHipX - midShoulderX
        
        // Returns angle in degrees
        return Math.toDegrees(Math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
    }
}
