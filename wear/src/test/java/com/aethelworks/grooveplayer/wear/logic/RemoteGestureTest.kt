package com.aethelworks.grooveplayer.wear.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteGestureTest {
    private val circle = Outline(100f, 100f, 100f, 100f, 100f)
    private val square = Outline(50f, 50f, 40f, 40f, 0f)

    @Test
    fun centerTapIsPlayPause() {
        val gesture = classify(100f, 100f, 101f, 100f, circle, 200f)
        assertEquals(RemoteGesture.PlayPause, gesture)
    }

    @Test
    fun swipeLeftIsNextAndSwipeRightIsPrevious() {
        assertEquals(RemoteGesture.Next, classify(100f, 100f, 50f, 100f, circle, 200f))
        assertEquals(RemoteGesture.Previous, classify(100f, 100f, 160f, 102f, circle, 200f))
    }

    @Test
    fun ringTapAndRingDragSeekInsteadOfChangingTracks() {
        val tap = classify(100f, 0f, 102f, 2f, circle, 200f) as RemoteGesture.Seek
        assertEquals(0f, tap.fraction, 0.05f)

        val drag = classify(100f, 0f, 200f, 100f, circle, 200f) as RemoteGesture.Seek
        assertEquals(0.25f, drag.fraction, 0.05f)
    }

    @Test
    fun squareRingUsesThePerimeterAndSquareSwipeStillChangesTracks() {
        val seek = classify(50f, 10f, 50f, 10f, square, 100f) as RemoteGesture.Seek
        assertEquals(0f, seek.fraction, 0.05f)
        assertEquals(RemoteGesture.Next, classify(50f, 50f, 20f, 50f, square, 100f))
    }

    @Test
    fun disconnectedAndOutsideTapsDoNothing() {
        assertEquals(
            RemoteGesture.Ignore,
            classify(100f, 100f, 101f, 100f, circle, 200f, enabled = false),
        )
        assertEquals(RemoteGesture.Ignore, classify(0f, 0f, 1f, 1f, circle, 200f))
        assertEquals(
            RemoteGesture.Ignore,
            classify(100f, 0f, 102f, 0f, circle, 200f, seekEnabled = false),
        )
    }

    private fun classify(
        downX: Float,
        downY: Float,
        upX: Float,
        upY: Float,
        outline: Outline,
        minSide: Float,
        seekEnabled: Boolean = true,
        enabled: Boolean = true,
    ): RemoteGesture = classifyRemoteGesture(
        downX, downY, upX, upY, outline, minSide, seekEnabled, enabled,
    )
}
