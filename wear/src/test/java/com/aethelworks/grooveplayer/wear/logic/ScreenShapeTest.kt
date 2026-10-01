package com.aethelworks.grooveplayer.wear.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenShapeTest {
    @Test
    fun reportedCornersWinOverTheRoundFlag() {
        assertEquals(
            28f,
            resolvedCornerRadiusPx(200f, 200f, listOf(20f, 36f), screenRound = true),
            0.01f,
        )
    }

    @Test
    fun roundFallbackIsHalfTheShorterSide() {
        assertEquals(90f, resolvedCornerRadiusPx(180f, 200f, emptyList(), screenRound = true), 0.01f)
    }

    @Test
    fun squareFallbackIsASharpRect() {
        assertEquals(0f, resolvedCornerRadiusPx(180f, 180f, emptyList(), screenRound = false), 0.01f)
    }

    @Test
    fun cornersAreClampedToACircle() {
        assertEquals(50f, resolvedCornerRadiusPx(100f, 120f, listOf(400f), screenRound = false), 0.01f)
    }

    @Test
    fun safeInsetIsCappedToAFractionOfTheScreen() {
        assertEquals(12f, cappedSafeInsetPx(40f, 100f), 0.01f)
        assertEquals(4f, cappedSafeInsetPx(4f, 100f), 0.01f)
    }
}
