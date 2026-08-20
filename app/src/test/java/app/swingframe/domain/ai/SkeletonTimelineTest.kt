package app.swingframe.domain.ai

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SkeletonTimelineTest {

    private val emptySkeleton = SwingSkeleton(
        leftShoulder = null, rightShoulder = null, leftElbow = null, rightElbow = null,
        leftWrist = null, rightWrist = null, leftHip = null, rightHip = null,
        leftKnee = null, rightKnee = null, leftAnkle = null, rightAnkle = null
    )

    @Test
    fun `empty timeline returns null`() {
        assertNull(SkeletonTimeline.EMPTY.skeletonAt(100L))
        assertNull(SkeletonTimeline.from(emptyMap()).skeletonAt(0L))
    }

    @Test
    fun `returns nearest skeleton by time`() {
        val at0 = emptySkeleton
        val at100 = emptySkeleton
        val at300 = emptySkeleton
        val timeline = SkeletonTimeline.from(linkedMapOf(0L to at0, 100L to at100, 300L to at300))

        assertSame(at100, timeline.skeletonAt(120L))    // 20 away vs 180/120
        assertSame(at0, timeline.skeletonAt(0L))
        assertSame(at300, timeline.skeletonAt(290L))
        assertSame(at300, timeline.skeletonAt(10_000L)) // clamps to last frame
    }
}
