package app.swingframe.annotation

/** Search radius, in frames, shared by every carry-forward surface (preview, still, video). */
const val CARRY_FORWARD_RADIUS = 3

/**
 * Resolves the carried ("ghost") vectors for [frameIndex]: the nearest frame within [radius]
 * that has annotations, checking past frames before future ones at each distance.
 *
 * This is the single carry-forward rule. The preview session, still export, and video export
 * must all resolve the same reference shapes for the same frame, or burned-in output silently
 * diverges from what the user composed against.
 */
fun carriedAnnotationsFor(
    annotationsByFrame: Map<Int, List<AnnotationShape>>,
    frameIndex: Int,
    radius: Int = CARRY_FORWARD_RADIUS,
): List<AnnotationShape> {
    for (distance in 1..radius) {
        annotationsByFrame[frameIndex - distance]?.takeIf { it.isNotEmpty() }?.let { return it }
        annotationsByFrame[frameIndex + distance]?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return emptyList()
}
