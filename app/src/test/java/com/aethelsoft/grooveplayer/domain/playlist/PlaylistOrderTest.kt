package com.aethelsoft.grooveplayer.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistOrderTest {

    @Test
    fun `move shifts an item and leaves invalid moves unchanged`() {
        val tracks = listOf("a", "b", "c", "d")

        assertEquals(listOf("b", "c", "a", "d"), PlaylistOrder.move(tracks, 0, 2))
        assertEquals(listOf("a", "c", "d", "b"), PlaylistOrder.move(tracks, 1, 3))
        assertEquals(tracks, PlaylistOrder.move(tracks, 1, 1))
        assertEquals(tracks, PlaylistOrder.move(tracks, -1, 0))
        assertEquals(tracks, PlaylistOrder.move(tracks, 0, 4))
    }
}
