package com.aethelsoft.grooveplayer.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearPhoneBridgeTest {
    @Test
    fun commandsMapOntoTheExistingTransport() {
        assertEquals(
            WearTransport.Pause,
            WearPlayerCommands.action(WearCommand.PlayPause, playing = true),
        )
        assertEquals(
            WearTransport.Play,
            WearPlayerCommands.action(WearCommand.PlayPause, playing = false),
        )
        assertEquals(WearTransport.Next, WearPlayerCommands.action(WearCommand.Next, playing = true))
        assertEquals(
            WearTransport.Previous,
            WearPlayerCommands.action(WearCommand.Previous, playing = false),
        )
        assertEquals(
            WearTransport.Seek(8_000L),
            WearPlayerCommands.action(WearCommand.Seek(8_000L), playing = true),
        )
        assertEquals(
            WearTransport.Republish,
            WearPlayerCommands.action(WearCommand.RequestState, playing = false),
        )
    }

    @Test
    fun pathsFromTheWatchBecomeThoseActions() {
        val seek = WearProtocol.parseCommand(WearProtocol.PATH_SEEK, WearProtocol.encodeSeek(12_500L))
        assertEquals(WearTransport.Seek(12_500L), WearPlayerCommands.action(seek!!, playing = true))
        assertNull(WearProtocol.parseCommand(WearProtocol.PATH_SEEK, byteArrayOf(1, 2)))
        assertEquals(4_000L, WearPlayerCommands.clampSeek(9_000L, 4_000L))
        assertEquals(0L, WearPlayerCommands.clampSeek(-20L, 4_000L))
        assertEquals(0L, WearPlayerCommands.clampSeek(20L, 0L))
    }

    @Test
    fun publishSkipsPositionTicksUnlessTheTrackPlayStateOrSeekChanges() {
        val playing = sample(playing = true, position = 1_000L)
        assertFalse(
            WearPublishPolicy.shouldPublish(
                watchConnected = false,
                previous = null,
                current = playing,
                nowMs = 10_000L,
                lastSentAtMs = 0L,
            ),
        )
        assertTrue(
            WearPublishPolicy.shouldPublish(true, null, playing, 10_000L, 0L),
        )
        assertFalse(
            WearPublishPolicy.shouldPublish(true, playing, playing.copy(positionMs = 1_400L), 10_400L, 10_000L),
        )
        assertTrue(
            WearPublishPolicy.shouldPublish(true, playing, playing.copy(songId = "other"), 10_200L, 10_000L),
        )
        assertTrue(
            WearPublishPolicy.shouldPublish(true, playing, playing.copy(isPlaying = false), 10_200L, 10_000L),
        )
        assertTrue(
            WearPublishPolicy.shouldPublish(true, playing, playing.copy(positionMs = 8_000L), 10_300L, 10_000L),
        )
        assertTrue(
            WearPublishPolicy.shouldPublish(true, playing, playing.copy(positionMs = 6_200L), 15_000L, 10_000L),
        )
        val paused = playing.copy(isPlaying = false, positionMs = 2_000L)
        assertFalse(
            WearPublishPolicy.shouldPublish(true, paused, paused.copy(positionMs = 2_050L), 20_000L, 10_000L),
        )
    }

    @Test
    fun artworkStaysInsideThePrivateLibraryAndWithin400Px() {
        val root = "/data/user/0/com.aethelsoft.grooveplayer/files/groove-library"
        assertTrue(WearArtworkPolicy.allowed("$root/song.mp3", root))
        assertFalse(WearArtworkPolicy.allowed("/storage/emulated/0/Music/song.mp3", root))
        assertFalse(WearArtworkPolicy.allowed(null, root))
        assertFalse(WearArtworkPolicy.allowed("$root/song.mp3", ""))
        assertEquals(400 to 300, WearArtworkPolicy.fittedSize(800, 600))
        assertEquals(400 to 400, WearArtworkPolicy.fittedSize(400, 400))
        assertEquals(20 to 400, WearArtworkPolicy.fittedSize(100, 2000))
        assertEquals(0 to 0, WearArtworkPolicy.fittedSize(0, 10))
        assertEquals(399 to 10, WearArtworkPolicy.fittedSize(399, 10))
        assertTrue(WearArtworkPolicy.fitsDataItem(20_000))
        assertFalse(WearArtworkPolicy.fitsDataItem(200_000))
    }

    private fun sample(playing: Boolean, position: Long) = WearPlaybackSample(
        songId = "song-1",
        title = "Midnight Radio",
        artist = "The Northern Line",
        isPlaying = playing,
        positionMs = position,
        durationMs = 200_000L,
    )
}
