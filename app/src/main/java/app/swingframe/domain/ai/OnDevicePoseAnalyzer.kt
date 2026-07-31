package app.swingframe.domain.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.tasks.await

class OnDevicePoseAnalyzer {

    // Using the "Accurate" model for golf swings rather than "Fast", 
    // because we need exact joint mapping to draw lines accurately.
    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
        .build()

    private val poseDetector: PoseDetector = PoseDetection.getClient(options)

    /**
     * Takes a single raw video frame bitmap and runs ML Kit locally to extract joints.
     */
    suspend fun analyzeFrame(bitmap: Bitmap): SwingSkeleton? {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        return try {
            val pose = poseDetector.process(image).await()
            mapToSwingSkeleton(pose, bitmap.width, bitmap.height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapToSwingSkeleton(pose: Pose, imageWidth: Int, imageHeight: Int): SwingSkeleton? {
        if (pose.allPoseLandmarks.isEmpty()) return null

        fun getJoint(type: Int): PoseJoint? {
            val landmark = pose.getPoseLandmark(type) ?: return null
            // ML Kit can sometimes return very low confidence points. We filter out the junk.
            if (landmark.inFrameLikelihood < 0.5f) return null
            
            // Normalize coordinates to 0.0 -> 1.0 based on image size 
            // so we can draw it perfectly regardless of screen size.
            return PoseJoint(
                x = landmark.position.x / imageWidth,
                y = landmark.position.y / imageHeight,
                confidence = landmark.inFrameLikelihood
            )
        }

        return SwingSkeleton(
            leftShoulder = getJoint(PoseLandmark.LEFT_SHOULDER),
            rightShoulder = getJoint(PoseLandmark.RIGHT_SHOULDER),
            leftElbow = getJoint(PoseLandmark.LEFT_ELBOW),
            rightElbow = getJoint(PoseLandmark.RIGHT_ELBOW),
            leftWrist = getJoint(PoseLandmark.LEFT_WRIST),
            rightWrist = getJoint(PoseLandmark.RIGHT_WRIST),
            leftHip = getJoint(PoseLandmark.LEFT_HIP),
            rightHip = getJoint(PoseLandmark.RIGHT_HIP),
            leftKnee = getJoint(PoseLandmark.LEFT_KNEE),
            rightKnee = getJoint(PoseLandmark.RIGHT_KNEE),
            leftAnkle = getJoint(PoseLandmark.LEFT_ANKLE),
            rightAnkle = getJoint(PoseLandmark.RIGHT_ANKLE)
        )
    }
}
