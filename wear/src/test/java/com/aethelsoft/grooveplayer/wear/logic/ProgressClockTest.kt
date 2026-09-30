package com.aethelsoft.grooveplayer.wear.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressClockTest {
    @Test
    fun playingAdvancesFromTheSampledPosition() {
        assertEquals(3_000L, interpolatedPositionMs(1_000L, true, 10_000L, 12_000L, 8_000L))
    }

    @Test
    fun playingClampsAtTheDuration() {
        assertEquals(5_000L, interpolatedPositionMs(1_000L, true, 10_000L, 20_000L, 5_000L))
    }

    @Test
    fun pausedIgnoresElapsedTime() {
        assertEquals(1_000L, interpolatedPositionMs(1_000L, false, 10_000L, 20_000L, 5_000L))
    }

    @Test
    fun aWatchClockBehindThePhoneDoesNotRewind() {
        assertEquals(1_000L, interpolatedPositionMs(1_000L, true, 10_000L, 9_000L, 5_000L))
    }

    @Test
    fun aMissingDurationStaysAtZero() {
        assertEquals(0L, interpolatedPositionMs(100L, true, 0L, 50L, 0L))
    }

    @Test
    fun fractionAndPositionRoundTrip() {
        assertEquals(0.5f, progressFraction(1_000L, 2_000L), 0.0001f)
        assertEquals(1_500L, positionForFraction(0.5f, 3_000L))
        assertEquals(0f, progressFraction(10L, 0L), 0f)
        assertEquals(0L, positionForFraction(0.4f, 0L))
    }

    @Test
    fun clockFormat() {
        assertEquals("1:23", formatClock(83_000L))
        assertEquals("3:34", formatClock(214_000L))
        assertEquals("1:01:01", formatClock(3_661_000L))
    }
}
