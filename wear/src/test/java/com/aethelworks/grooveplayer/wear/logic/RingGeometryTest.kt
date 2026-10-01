package com.aethelworks.grooveplayer.wear.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class RingGeometryTest {
    @Test
    fun circleMapsAngleToPositionClockwiseFromTheTop() {
        val circle = Outline(50f, 50f, 50f, 50f, 50f)
        assertNear(50f, 0f, circle.pointAtFraction(0f))
        assertNear(100f, 50f, circle.pointAtFraction(0.25f))
        assertNear(50f, 100f, circle.pointAtFraction(0.5f))
        assertNear(0f, 50f, circle.pointAtFraction(0.75f))

        val fromTop = 22.5 * PI / 180.0
        val theta = (-PI / 2.0) + fromTop
        val x = 50.0 + 50.0 * cos(theta)
        val y = 50.0 + 50.0 * sin(theta)
        assertEquals(22.5f / 360f, circle.fractionAt(x.toFloat(), y.toFloat()), 0.02f)
        assertEquals(0.25f, circle.fractionAt(100f, 50f), 0.02f)
    }

    @Test
    fun squareMapsPerimeterPositionRatherThanVisualAngle() {
        val square = Outline(50f, 50f, 50f, 50f, 0f)
        assertNear(50f, 0f, square.pointAtFraction(0f))
        assertNear(100f, 0f, square.pointAtFraction(0.125f))
        assertNear(100f, 50f, square.pointAtFraction(0.25f))
        assertNear(50f, 100f, square.pointAtFraction(0.5f))
        assertNear(0f, 50f, square.pointAtFraction(0.75f))

        // 20.7px along the top edge is not the same fraction as the angle of that ray.
        val alongTop = square.fractionAt(70.7f, 0f)
        assertEquals(20.7f / 400f, alongTop, 0.015f)
        val angleFraction = 22.5f / 360f
        assert(kotlin.math.abs(alongTop - angleFraction) > 0.005f)
    }

    @Test
    fun roundedRectRoundTripsArcLength() {
        val rect = Outline(120f, 100f, 100f, 70f, 24f)
        for (fraction in listOf(0f, 0.1f, 0.25f, 0.33f, 0.5f, 0.67f, 0.9f)) {
            val point = rect.pointAtFraction(fraction)!!
            assertEquals(fraction, rect.fractionAt(point.first, point.second), 0.02f)
        }
    }

    @Test
    fun centerlineOfARoundScreenStaysCircular() {
        val outline = centerlineOutline(
            widthPx = 200f,
            heightPx = 200f,
            screenCornerRadiusPx = 100f,
            safeInsetPx = 0f,
        )
        val top = outline.pointAtFraction(0f)!!
        val right = outline.pointAtFraction(0.25f)!!
        assertEquals(outline.centerX, top.first, 1.5f)
        assert(top.second < outline.centerY)
        assert(right.first > outline.centerX)
        assertEquals(outline.centerY, right.second, 1.5f)
    }

    @Test
    fun centerlineOfASquareScreenStaysRectangular() {
        val outline = centerlineOutline(
            widthPx = 180f,
            heightPx = 180f,
            screenCornerRadiusPx = 0f,
            safeInsetPx = 0f,
        )
        assertEquals(0f, outline.cornerRadius, 0.01f)
        val right = outline.pointAtFraction(0.25f)!!
        assert(right.first > outline.centerX)
        assertEquals(outline.centerY, right.second, 1.5f)
    }

    private fun assertNear(x: Float, y: Float, point: Pair<Float, Float>?) {
        val (px, py) = point ?: error("missing point")
        assertEquals(x, px, 1.5f)
        assertEquals(y, py, 1.5f)
    }
}
