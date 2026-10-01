package com.aethelworks.grooveplayer.wear.logic

import kotlin.math.abs
import kotlin.math.hypot

sealed interface RemoteGesture {
    data object PlayPause : RemoteGesture
    data object Next : RemoteGesture
    data object Previous : RemoteGesture
    data class Seek(val fraction: Float) : RemoteGesture
    data object Ignore : RemoteGesture
}

/**
 * Swipe left (finger moves left) is next. Swipe right is previous.
 * A gesture that starts on the ring is a seek, including a drag that also
 * travels sideways. [minSidePx] is the shorter measured side, so the swipe
 * and tap thresholds grow with the watch.
 */
fun classifyRemoteGesture(
    downX: Float,
    downY: Float,
    upX: Float,
    upY: Float,
    outline: Outline,
    minSidePx: Float,
    seekEnabled: Boolean,
    enabled: Boolean,
): RemoteGesture {
    if (!enabled) return RemoteGesture.Ignore
    val span = minSidePx.coerceAtLeast(1f)
    val hitSlop = span * RemoteMetrics.HIT_FRACTION
    val onRing = outline.distanceTo(downX, downY) <= hitSlop
    val inside = outline.contains(downX, downY)
    if (!onRing && !inside) return RemoteGesture.Ignore
    if (onRing) {
        if (!seekEnabled) return RemoteGesture.Ignore
        return RemoteGesture.Seek(outline.fractionAt(upX, upY))
    }
    val dx = upX - downX
    val dy = upY - downY
    val swipe = span * RemoteMetrics.SWIPE_FRACTION
    if (abs(dx) >= swipe && abs(dx) > abs(dy) * 1.15f) {
        return if (dx < 0f) RemoteGesture.Next else RemoteGesture.Previous
    }
    if (hypot(dx, dy) <= span * RemoteMetrics.TAP_FRACTION) {
        return RemoteGesture.PlayPause
    }
    return RemoteGesture.Ignore
}
