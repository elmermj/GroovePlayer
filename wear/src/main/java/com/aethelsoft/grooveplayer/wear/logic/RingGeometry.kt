package com.aethelsoft.grooveplayer.wear.logic

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Centerline of the progress ring. A circle is a rounded rect whose corner
 * radius meets both halves. A square is the same shape with a zero radius.
 * Progress runs clockwise from the top center, by arc length.
 */
data class Outline(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float,
)

internal sealed interface RingSegment {
    val length: Float

    fun pointAt(t: Float): Pair<Float, Float>

    fun closest(x: Float, y: Float): Pair<Float, Float>

    data class Line(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        override val length: Float,
    ) : RingSegment {
        override fun pointAt(t: Float): Pair<Float, Float> {
            val clamped = t.coerceIn(0f, 1f)
            return x0 + (x1 - x0) * clamped to y0 + (y1 - y0) * clamped
        }

        override fun closest(x: Float, y: Float): Pair<Float, Float> {
            val dx = x1 - x0
            val dy = y1 - y0
            val len2 = dx * dx + dy * dy
            if (len2 <= 1e-4f) return 0f to hypot(x - x0, y - y0)
            val t = (((x - x0) * dx + (y - y0) * dy) / len2).coerceIn(0f, 1f)
            val point = pointAt(t)
            return t to hypot(x - point.first, y - point.second)
        }
    }

    data class Arc(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val startAngle: Float,
        val sweep: Float,
        override val length: Float,
    ) : RingSegment {
        override fun pointAt(t: Float): Pair<Float, Float> {
            val angle = startAngle + sweep * t.coerceIn(0f, 1f)
            return cx + radius * cos(angle) to cy + radius * sin(angle)
        }

        override fun closest(x: Float, y: Float): Pair<Float, Float> {
            if (sweep <= 0f || radius <= 0f) {
                val point = pointAt(0f)
                return 0f to hypot(x - point.first, y - point.second)
            }
            val angle = clampToSweep(atan2(y - cy, x - cx), startAngle, sweep)
            val t = ((angle - startAngle) / sweep).coerceIn(0f, 1f)
            val point = pointAt(t)
            return t to hypot(x - point.first, y - point.second)
        }
    }
}

internal fun ringSegments(outline: Outline): List<RingSegment> {
    val radius = outline.cornerRadius.coerceIn(
        0f,
        min(outline.halfWidth, outline.halfHeight),
    )
    val cx = outline.centerX
    val cy = outline.centerY
    val left = cx - outline.halfWidth
    val right = cx + outline.halfWidth
    val top = cy - outline.halfHeight
    val bottom = cy + outline.halfHeight
    if (right - left <= 0f || bottom - top <= 0f) return emptyList()

    val segments = mutableListOf<RingSegment>()
    fun line(x0: Float, y0: Float, x1: Float, y1: Float) {
        val length = hypot(x1 - x0, y1 - y0)
        if (length > 0.01f) segments += RingSegment.Line(x0, y0, x1, y1, length)
    }
    fun arc(arcX: Float, arcY: Float, startAngle: Float) {
        if (radius <= 0.01f) return
        val sweep = (PI / 2.0).toFloat()
        segments += RingSegment.Arc(arcX, arcY, radius, startAngle, sweep, radius * sweep)
    }

    line(cx, top, right - radius, top)
    arc(right - radius, top + radius, (-PI / 2.0).toFloat())
    line(right, top + radius, right, bottom - radius)
    arc(right - radius, bottom - radius, 0f)
    line(right - radius, bottom, left + radius, bottom)
    arc(left + radius, bottom - radius, (PI / 2.0).toFloat())
    line(left, bottom - radius, left, top + radius)
    arc(left + radius, top + radius, PI.toFloat())
    line(left + radius, top, cx, top)
    return segments
}

fun centerlineOutline(
    widthPx: Float,
    heightPx: Float,
    screenCornerRadiusPx: Float,
    safeInsetPx: Float,
): Outline {
    val minSide = min(widthPx, heightPx).coerceAtLeast(0f)
    val stroke = strokeWidthPx(widthPx, heightPx)
    val inset = maxOf(cappedSafeInsetPx(safeInsetPx, minSide), stroke / 2f)
    val innerWidth = (widthPx - 2f * inset).coerceAtLeast(0f)
    val innerHeight = (heightPx - 2f * inset).coerceAtLeast(0f)
    return Outline(
        centerX = widthPx / 2f,
        centerY = heightPx / 2f,
        halfWidth = innerWidth / 2f,
        halfHeight = innerHeight / 2f,
        cornerRadius = (screenCornerRadiusPx - inset).coerceAtLeast(0f),
    )
}

fun Outline.pointAtFraction(fraction: Float): Pair<Float, Float>? {
    val segments = ringSegments(this)
    val perimeter = segments.sumOf { it.length.toDouble() }.toFloat()
    if (segments.isEmpty() || perimeter <= 0f) return null
    val target = fraction.coerceIn(0f, 1f) * perimeter
    var walked = 0f
    for (segment in segments) {
        val next = walked + segment.length
        if (target <= next + 0.001f || segment === segments.last()) {
            val t = if (segment.length <= 0f) 0f else ((target - walked) / segment.length).coerceIn(0f, 1f)
            return segment.pointAt(t)
        }
        walked = next
    }
    return segments.last().pointAt(1f)
}

fun Outline.fractionAt(x: Float, y: Float): Float {
    val segments = ringSegments(this)
    val perimeter = segments.sumOf { it.length.toDouble() }.toFloat()
    if (segments.isEmpty() || perimeter <= 0f) return 0f
    var bestDistance = Float.POSITIVE_INFINITY
    var best = 0f
    var walked = 0f
    for (segment in segments) {
        val (t, distance) = segment.closest(x, y)
        if (distance < bestDistance) {
            bestDistance = distance
            best = (walked + t * segment.length) / perimeter
        }
        walked += segment.length
    }
    return best.coerceIn(0f, 1f)
}

fun Outline.distanceTo(x: Float, y: Float): Float {
    val segments = ringSegments(this)
    if (segments.isEmpty()) return Float.POSITIVE_INFINITY
    var best = Float.POSITIVE_INFINITY
    for (segment in segments) {
        val distance = segment.closest(x, y).second
        if (distance < best) best = distance
    }
    return best
}

fun Outline.contains(x: Float, y: Float): Boolean {
    val radius = cornerRadius.coerceIn(0f, min(halfWidth, halfHeight))
    val dx = abs(x - centerX)
    val dy = abs(y - centerY)
    if (dx > halfWidth || dy > halfHeight) return false
    if (dx <= halfWidth - radius || dy <= halfHeight - radius) return true
    val cornerX = dx - (halfWidth - radius)
    val cornerY = dy - (halfHeight - radius)
    return cornerX * cornerX + cornerY * cornerY <= radius * radius
}

private fun clampToSweep(angle: Float, start: Float, sweep: Float): Float {
    val end = start + sweep
    val turn = (PI * 2.0).toFloat()
    var value = angle
    while (value < start) value += turn
    while (value >= start + turn) value -= turn
    return value.coerceIn(start, end)
}
