package com.aethelsoft.grooveplayer.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearProtocolTest {
    @Test
    fun pathsAndKeysMatchTheContract() {
        assertEquals("grooveplayer_phone", WearProtocol.CAPABILITY_PHONE)
        assertEquals("grooveplayer_watch", WearProtocol.CAPABILITY_WATCH)
        assertEquals("/grooveplayer/state", WearProtocol.PATH_STATE)
        assertEquals("/grooveplayer/cmd/play_pause", WearProtocol.PATH_PLAY_PAUSE)
        assertEquals("/grooveplayer/cmd/next", WearProtocol.PATH_NEXT)
        assertEquals("/grooveplayer/cmd/previous", WearProtocol.PATH_PREVIOUS)
        assertEquals("/grooveplayer/cmd/seek", WearProtocol.PATH_SEEK)
        assertEquals("/grooveplayer/cmd/request_state", WearProtocol.PATH_REQUEST_STATE)
        assertEquals("title", WearProtocol.KEY_TITLE)
        assertEquals("artist", WearProtocol.KEY_ARTIST)
        assertEquals("isPlaying", WearProtocol.KEY_IS_PLAYING)
        assertEquals("positionMs", WearProtocol.KEY_POSITION_MS)
        assertEquals("durationMs", WearProtocol.KEY_DURATION_MS)
        assertEquals("updatedAtMs", WearProtocol.KEY_UPDATED_AT_MS)
        assertEquals("artwork", WearProtocol.KEY_ARTWORK)
        assertEquals(400, WearProtocol.ARTWORK_MAX_EDGE_PX)
    }

    @Test
    fun seekIsEightByteBigEndian() {
        val bytes = WearProtocol.encodeSeek(0x0102030405060708L)
        assertEquals(8, bytes.size)
        assertEquals(0x01, bytes[0].toInt() and 0xFF)
        assertEquals(0x08, bytes[7].toInt() and 0xFF)
        assertEquals(0x0102030405060708L, WearProtocol.decodeSeek(bytes))
        assertNull(WearProtocol.decodeSeek(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun emptyCommandsParseAndJunkDoesNot() {
        assertEquals(WearCommand.PlayPause, WearProtocol.parseCommand(WearProtocol.PATH_PLAY_PAUSE, byteArrayOf()))
        assertEquals(WearCommand.Next, WearProtocol.parseCommand(WearProtocol.PATH_NEXT, null))
        assertEquals(WearCommand.Previous, WearProtocol.parseCommand(WearProtocol.PATH_PREVIOUS, byteArrayOf()))
        assertEquals(
            WearCommand.RequestState,
            WearProtocol.parseCommand(WearProtocol.PATH_REQUEST_STATE, byteArrayOf()),
        )
        assertTrue(WearProtocol.parseCommand(WearProtocol.PATH_SEEK, WearProtocol.encodeSeek(1500L)) is WearCommand.Seek)
        assertNull(WearProtocol.parseCommand(WearProtocol.PATH_PLAY_PAUSE, byteArrayOf(1)))
        assertNull(WearProtocol.parseCommand("/grooveplayer/cmd/other", byteArrayOf()))
    }
}
