package com.aethelworks.grooveplayer.data.player

import com.aethelworks.grooveplayer.domain.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackCrossfadeTest {

    @Test
    fun indices_followRepeatTheWayExoPlayerNavigates() {
        assertNull(TrackCrossfade.autoNextIndex(0, 3, RepeatMode.ONE))
        assertEquals(1, TrackCrossfade.skipNextIndex(0, 3, RepeatMode.ONE))
        assertNull(TrackCrossfade.skipNextIndex(2, 3, RepeatMode.ONE))

        assertEquals(1, TrackCrossfade.autoNextIndex(0, 3, RepeatMode.OFF))
        assertNull(TrackCrossfade.autoNextIndex(2, 3, RepeatMode.OFF))
        assertNull(TrackCrossfade.skipPreviousIndex(0, 3, RepeatMode.OFF))

        assertEquals(0, TrackCrossfade.autoNextIndex(2, 3, RepeatMode.ALL))
        assertEquals(2, TrackCrossfade.skipPreviousIndex(0, 3, RepeatMode.ALL))
        assertNull(TrackCrossfade.autoNextIndex(0, 1, RepeatMode.ALL))
    }

    @Test
    fun effectiveFade_fitsTheTrackAndRejectsTinyOverlaps() {
        assertEquals(0L, TrackCrossfade.effectiveFadeMs(5_000L, 0L))
        assertEquals(0L, TrackCrossfade.effectiveFadeMs(0L, 180_000L))
        assertEquals(5_000L, TrackCrossfade.effectiveFadeMs(5_000L, 180_000L))
        assertEquals(1_800L, TrackCrossfade.effectiveFadeMs(10_000L, 2_000L))
        assertEquals(0L, TrackCrossfade.effectiveFadeMs(10_000L, 500L))
        assertEquals(1_000L, TrackCrossfade.manualFadeMs(5_000L, 1_000L))
        assertEquals(0L, TrackCrossfade.manualFadeMs(5_000L, 300L))
    }

    @Test
    fun pauseAtEnd_onlyWhenAnotherTrackCanFadeIn() {
        assertFalse(
            TrackCrossfade.shouldPauseAtEnd(0, 180_000L, 0, 3, RepeatMode.OFF),
        )
        assertFalse(
            TrackCrossfade.shouldPauseAtEnd(5, 180_000L, 2, 3, RepeatMode.OFF),
        )
        assertFalse(
            TrackCrossfade.shouldPauseAtEnd(5, 180_000L, 0, 3, RepeatMode.ONE),
        )
        assertFalse(
            TrackCrossfade.shouldPauseAtEnd(5, 0L, 0, 3, RepeatMode.OFF),
        )
        assertTrue(
            TrackCrossfade.shouldPauseAtEnd(5, 180_000L, 0, 3, RepeatMode.OFF),
        )
        assertTrue(
            TrackCrossfade.shouldPauseAtEnd(5, 180_000L, 2, 3, RepeatMode.ALL),
        )
    }

    @Test
    fun equalPower_holdsLoudnessAcrossTheOverlap() {
        val start = TrackCrossfade.equalPowerGains(0L, 4_000L, 1f)
        assertEquals(1f, start.first, 0.001f)
        assertEquals(0f, start.second, 0.001f)

        val mid = TrackCrossfade.equalPowerGains(2_000L, 4_000L, 1f)
        assertEquals(0.7071f, mid.first, 0.001f)
        assertEquals(0.7071f, mid.second, 0.001f)

        val end = TrackCrossfade.equalPowerGains(4_000L, 4_000L, 0.5f)
        assertEquals(0f, end.first, 0.001f)
        assertEquals(0.5f, end.second, 0.001f)
    }

    @Test
    fun naturalPlan_preloadsThenOverlapsAndHardCutsIfLate() {
        val far = plan(positionMs = 100_000L)
        assertEquals(TrackCrossfade.Step.None, far)

        val preload = plan(positionMs = 174_000L)
        assertEquals(TrackCrossfade.Step.PreloadOutgoing, preload)

        val waiting = plan(positionMs = 176_000L, phase = TrackCrossfade.Phase.PRELOADING)
        assertEquals(TrackCrossfade.Step.None, waiting)

        val begin = plan(
            positionMs = 176_000L,
            phase = TrackCrossfade.Phase.PRELOADING,
            outgoingReady = true,
        )
        assertEquals(TrackCrossfade.Step.Begin(nextIndex = 1, fadeMs = 4_000L), begin)

        val late = plan(
            positionMs = 179_900L,
            phase = TrackCrossfade.Phase.PRELOADING,
            outgoingReady = false,
        )
        assertEquals(TrackCrossfade.Step.HardCut(1), late)

        val missed = plan(positionMs = 180_000L, heldAtEnd = true, playWhenReady = false)
        assertEquals(TrackCrossfade.Step.HardCut(1), missed)
    }

    @Test
    fun naturalPlan_cancelsPreloadWhenTheWindowCloses() {
        val seekBack = plan(positionMs = 10_000L, phase = TrackCrossfade.Phase.PRELOADING)
        assertEquals(TrackCrossfade.Step.CancelOutgoing, seekBack)

        val repeatOne = plan(repeatMode = RepeatMode.ONE, phase = TrackCrossfade.Phase.PRELOADING)
        assertEquals(TrackCrossfade.Step.CancelOutgoing, repeatOne)

        val last = plan(currentIndex = 2, positionMs = 176_000L)
        assertEquals(TrackCrossfade.Step.None, last)

        val paused = plan(positionMs = 176_000L, playWhenReady = false, outgoingReady = true)
        assertEquals(TrackCrossfade.Step.None, paused)
    }

    @Test
    fun overlapPlan_waitsForTheIncomingTrackThenRamps() {
        val holding = TrackCrossfade.planOverlap(
            incomingPositionMs = 0L,
            stallMs = 200L,
            fadeDurationMs = 4_000L,
            audible = 1f,
            playWhenReady = true,
            incomingPlaying = false,
        )
        assertEquals(TrackCrossfade.Step.Volumes(outgoing = 1f, incoming = 0f), holding)

        val mid = TrackCrossfade.planOverlap(
            incomingPositionMs = 2_000L,
            stallMs = 2_000L,
            fadeDurationMs = 4_000L,
            audible = 1f,
            playWhenReady = true,
            incomingPlaying = true,
        )
        val midVolumes = mid as TrackCrossfade.Step.Volumes
        assertEquals(0.7071f, midVolumes.outgoing, 0.001f)
        assertEquals(0.7071f, midVolumes.incoming, 0.001f)

        assertEquals(
            TrackCrossfade.Step.Finish,
            TrackCrossfade.planOverlap(
                incomingPositionMs = 4_000L,
                stallMs = 4_000L,
                fadeDurationMs = 4_000L,
                audible = 1f,
                playWhenReady = true,
                incomingPlaying = true,
            ),
        )
        assertEquals(
            TrackCrossfade.Step.Finish,
            TrackCrossfade.planOverlap(
                incomingPositionMs = 0L,
                stallMs = TrackCrossfade.ARM_TIMEOUT_MS,
                fadeDurationMs = 4_000L,
                audible = 1f,
                playWhenReady = true,
                incomingPlaying = false,
            ),
        )
        assertEquals(
            TrackCrossfade.Step.None,
            TrackCrossfade.planOverlap(
                incomingPositionMs = 1_000L,
                stallMs = 1_000L,
                fadeDurationMs = 4_000L,
                audible = 1f,
                playWhenReady = false,
                incomingPlaying = false,
            ),
        )
    }

    private fun plan(
        positionMs: Long = 0L,
        playWhenReady: Boolean = true,
        heldAtEnd: Boolean = false,
        currentIndex: Int = 0,
        repeatMode: RepeatMode = RepeatMode.OFF,
        phase: TrackCrossfade.Phase = TrackCrossfade.Phase.IDLE,
        outgoingReady: Boolean = false,
    ): TrackCrossfade.Step {
        return TrackCrossfade.planNatural(
            TrackCrossfade.NaturalInput(
                fadeDurationMs = 5_000L,
                positionMs = positionMs,
                durationMs = 180_000L,
                playWhenReady = playWhenReady,
                heldAtEnd = heldAtEnd,
                currentIndex = currentIndex,
                itemCount = 3,
                repeatMode = repeatMode,
                phase = phase,
                outgoingReady = outgoingReady,
            ),
        )
    }
}
