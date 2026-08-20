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

/**
 * Draws the detected skeleton over the video.
 *
 * Skeleton coordinates are normalized over the UPRIGHT VIDEO frame. PlayerView renders
 * with aspect-fit, so when the video aspect differs from the overlay canvas there are
 * letterbox bars; the mapping below skips those bars or the skeleton would be stretched
 * or squashed against the visible video content.
 *
 * @param videoWidth/videoHeight display dimensions of the video as reported by the player
 *   (after rotation); pass 0/0 to fall back to full-canvas mapping.
 */
@Composable
fun SkeletonOverlay(
    skeleton: SwingSkeleton?,
    modifier: Modifier = Modifier,
    drawSpineLine: Boolean = true,
    videoWidth: Int = 0,
    videoHeight: Int = 0
) {
    if (skeleton == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Letterbox-aware mapping: normalized video coordinates -> canvas pixels
        val toCanvas: (Float, Float) -> Offset = if (videoWidth > 0 && videoHeight > 0) {
            val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
            val canvasAspect = w / h
            if (videoAspect > canvasAspect) {
                // Video is wider than the canvas -> pillarbox bars on left/right
                val drawWidth = h * videoAspect
                val leftInset = (w - drawWidth) / 2f
                { x, y -> Offset(leftInset + x * drawWidth, y * h) }
            } else {
                // Video is taller than the canvas -> letterbox bars on top/bottom
                val drawHeight = w / videoAspect
                val topInset = (h - drawHeight) / 2f
                { x, y -> Offset(x * w, topInset + y * drawHeight) }
            }
        } else {
            { x, y -> Offset(x * w, y * h) }
        }

        fun PoseJoint.toOffset(): Offset = toCanvas(x, y)

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

        // Analytics: spine line (Mustard Green) — reuses SwingSkeleton's midpoint math so
        // the drawn line and the analyzed angle can never disagree.
        if (drawSpineLine) {
            val midShoulder = skeleton.midShoulder
            val midHip = skeleton.midHip
            if (midShoulder != null && midHip != null) {
                drawLine(
                    color = MustardGreen,
                    start = midShoulder.toOffset(),
                    end = midHip.toOffset(),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Joint points on top
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
