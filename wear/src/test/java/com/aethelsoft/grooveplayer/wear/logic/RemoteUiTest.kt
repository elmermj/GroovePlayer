package com.aethelsoft.grooveplayer.wear.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteUiTest {
    private val playing = WatchSnapshot(
        title = "Midnight Radio",
        artist = "The Northern Line",
        isPlaying = true,
        positionMs = 1_000L,
        durationMs = 10_000L,
        updatedAtMs = 5_000L,
    )

    @Test
    fun disconnectedClearsTransportEvenIfASnapshotIsCached() {
        val ui = remoteUi(playing, connected = false, nowMs = 8_000L)
        assertFalse(ui.connected)
        assertEquals("Disconnected", ui.title)
        assertFalse(ui.showTransport)
        assertFalse(ui.isPlaying)
        assertEquals(0L, ui.positionMs)
    }

    @Test
    fun connectedPlayingInterpolatesAndPausedDoesNot() {
        val ui = remoteUi(playing, connected = true, nowMs = 7_500L)
        assertTrue(ui.showTransport)
        assertEquals(3_500L, ui.positionMs)
        assertTrue(ui.isPlaying)

        val paused = remoteUi(playing.copy(isPlaying = false), connected = true, nowMs = 9_000L)
        assertEquals(1_000L, paused.positionMs)
        assertFalse(paused.isPlaying)
    }

    @Test
    fun waitingAndBlankTitle() {
        val waiting = remoteUi(null, connected = true, nowMs = 0L)
        assertTrue(waiting.connected)
        assertEquals("Waiting for the phone", waiting.artist)
        assertFalse(waiting.showTransport)

        val blank = remoteUi(
            playing.copy(title = "  ", durationMs = 0L, positionMs = 0L),
            connected = true,
            nowMs = playing.updatedAtMs,
        )
        assertEquals("Nothing playing", blank.title)
        assertFalse(blank.showTransport)
    }
}
