package com.aethelsoft.grooveplayer.wear.logic

import kotlin.math.min

/**
 * Fractions of the shorter screen side. Layout sizes come from these and from
 * the measured bounds, not from a fixed dp value.
 */
object RemoteMetrics {
    const val STROKE_FRACTION = 0.045f
    const val HIT_FRACTION = 0.09f
    const val SWIPE_FRACTION = 0.22f
    const val TAP_FRACTION = 0.04f
    const val TITLE_FRACTION = 0.078f
    const val ARTIST_FRACTION = 0.052f
    const val TIME_FRACTION = 0.044f
    const val MARK_FRACTION = 0.11f
    const val TEXT_INSET_FRACTION = 0.18f
    /** Cap a reported bezel inset so a bad inset cannot swallow the ring. */
    const val INSET_CAP_FRACTION = 0.12f
}

fun strokeWidthPx(widthPx: Float, heightPx: Float): Float =
    min(widthPx, heightPx).coerceAtLeast(0f) * RemoteMetrics.STROKE_FRACTION

/**
 * Platform corner radii win. A round screen with no radii (API 30, or a preview
 * device) uses half the shorter side, which is a circle. A square screen stays
 * a rectangle until the platform reports a corner radius.
 */
fun resolvedCornerRadiusPx(
    widthPx: Float,
    heightPx: Float,
    reportedCornerRadiiPx: List<Float>,
    screenRound: Boolean,
): Float {
    val cap = min(widthPx, heightPx) / 2f
    if (cap <= 0f) return 0f
    val reported = reportedCornerRadiiPx.filter { it > 0f }
    if (reported.isNotEmpty()) {
        val average = reported.sum() / reported.size.toFloat()
        return average.coerceAtMost(cap)
    }
    return if (screenRound) cap else 0f
}

fun cappedSafeInsetPx(safeInsetPx: Float, minSidePx: Float): Float {
    val cap = minSidePx.coerceAtLeast(0f) * RemoteMetrics.INSET_CAP_FRACTION
    return safeInsetPx.coerceIn(0f, cap)
}
