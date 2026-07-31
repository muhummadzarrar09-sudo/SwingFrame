package app.swingframe.presentation.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import app.swingframe.domain.ai.PoseJoint
import app.swingframe.domain.ai.SwingSkeleton
import app.swingframe.ui.theme.MoonstoneBlue
import app.swingframe.ui.theme.MustardGreen

@Composable
fun SkeletonOverlay(
    skeleton: SwingSkeleton?,
    modifier: Modifier = Modifier,
    drawSpineLine: Boolean = true
) {
    if (skeleton == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Helper to convert normalized 0.0-1.0 coords back to screen pixels
        fun PoseJoint.toOffset(): Offset = Offset(x * w, y * h)

        fun drawBone(joint1: PoseJoint?, joint2: PoseJoint?, color: Color = MoonstoneBlue) {
            if (joint1 != null && joint2 != null) {
                drawLine(
                    color = color,
                    start = joint1.toOffset(),
                    end = joint2.toOffset(),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        fun drawJointPoint(joint: PoseJoint?) {
            if (joint != null) {
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = joint.toOffset()
                )
            }
        }

        // Draw the main skeletal structure
        
        // Arms
        drawBone(skeleton.leftShoulder, skeleton.leftElbow)
        drawBone(skeleton.leftElbow, skeleton.leftWrist)
        drawBone(skeleton.rightShoulder, skeleton.rightElbow)
        drawBone(skeleton.rightElbow, skeleton.rightWrist)

        // Torso Box
        drawBone(skeleton.leftShoulder, skeleton.rightShoulder)
        drawBone(skeleton.leftShoulder, skeleton.leftHip)
        drawBone(skeleton.rightShoulder, skeleton.rightHip)
        drawBone(skeleton.leftHip, skeleton.rightHip)

        // Legs
        drawBone(skeleton.leftHip, skeleton.leftKnee)
        drawBone(skeleton.leftKnee, skeleton.leftAnkle)
        drawBone(skeleton.rightHip, skeleton.rightKnee)
        drawBone(skeleton.rightKnee, skeleton.rightAnkle)

        // Draw the Analytics: Spine Angle Line (Mustard Green)
        if (drawSpineLine) {
            val ls = skeleton.leftShoulder
            val rs = skeleton.rightShoulder
            val lh = skeleton.leftHip
            val rh = skeleton.rightHip

            if (ls != null && rs != null && lh != null && rh != null) {
                val midShoulderX = ((ls.x + rs.x) / 2) * w
                val midShoulderY = ((ls.y + rs.y) / 2) * h
                val midHipX = ((lh.x + rh.x) / 2) * w
                val midHipY = ((lh.y + rh.y) / 2) * h

                // Draw a thick line connecting mid-shoulders to mid-hips
                drawLine(
                    color = MustardGreen,
                    start = Offset(midShoulderX, midShoulderY),
                    end = Offset(midHipX, midHipY),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw the physical points on top
        listOf(
            skeleton.leftShoulder, skeleton.rightShoulder,
            skeleton.leftElbow, skeleton.rightElbow,
            skeleton.leftWrist, skeleton.rightWrist,
            skeleton.leftHip, skeleton.rightHip,
            skeleton.leftKnee, skeleton.rightKnee,
            skeleton.leftAnkle, skeleton.rightAnkle
        ).forEach { drawJointPoint(it) }
    }
}
