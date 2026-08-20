package app.swingframe.domain.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class OnDevicePoseAnalyzer {

    // "Accurate" model for golf swings: we need exact joint mapping for the overlay.
    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
        .build()

    private val poseDetector: PoseDetector = PoseDetection.getClient(options)

    /**
     * Runs ML Kit locally on a single frame.
     *
     * @param rotationDegrees clockwise rotation of the frame as reported by
     *   MediaMetadataRetriever's METADATA_KEY_VIDEO_ROTATION (0/90/180/270).
     *   MediaMetadataRetriever returns frames in the ENCODED orientation without applying
     *   this metadata, while ExoPlayer DOES rotate during playback — so the rotation must
     *   be forwarded here or the skeleton would land 90 degrees off the visible video.
     */
    suspend fun analyzeFrame(bitmap: Bitmap, rotationDegrees: Int = 0): SwingSkeleton? {
        val rotation = normalizeRotation(rotationDegrees)
        val image = InputImage.fromBitmap(bitmap, rotation)

        // ML Kit returns landmarks in the coordinate space of the ROTATED (upright) image,
        // so normalization must use the rotated dimensions, not the raw bitmap dimensions.
        val rotatedWidth = if (rotation % 180 == 0) bitmap.width else bitmap.height
        val rotatedHeight = if (rotation % 180 == 0) bitmap.height else bitmap.width

        return try {
            val pose = poseDetector.process(image).await()
            mapToSwingSkeleton(pose, rotatedWidth, rotatedHeight)
        } catch (e: CancellationException) {
            throw e // never swallow cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Pose detection failed", e)
            null
        }
    }

    fun close() {
        poseDetector.close()
    }

    private fun mapToSwingSkeleton(pose: Pose, imageWidth: Int, imageHeight: Int): SwingSkeleton? {
        if (pose.allPoseLandmarks.isEmpty()) return null

        fun getJoint(type: Int): PoseJoint? {
            val landmark = pose.getPoseLandmark(type) ?: return null
            // Filter out low-confidence points so the overlay doesn't draw junk.
            if (landmark.inFrameLikelihood < 0.5f) return null

            // Normalized 0.0 -> 1.0 coordinates so the overlay can draw at any screen size.
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

    /** Clamps any input to the 0/90/180/270 quadrant ML Kit accepts. */
    private fun normalizeRotation(rotationDegrees: Int): Int {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        return when (normalized) {
            in 0..89 -> 0
            in 90..179 -> 90
            in 180..269 -> 180
            else -> 270
        }
    }

    private companion object {
        const val TAG = "OnDevicePoseAnalyzer"
    }
}
