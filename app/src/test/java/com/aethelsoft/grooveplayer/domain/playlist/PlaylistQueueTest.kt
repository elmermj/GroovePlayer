package com.aethelsoft.grooveplayer.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistQueueTest {

    @Test
    fun playButtonStartsAtTheFirstPlayableRow() {
        assertEquals(0, playlistQueueStart(listOf("a", "b"), tappedIndex = null, playableIds = listOf("b")))
        assertEquals(-1, playlistQueueStart(listOf("a"), tappedIndex = null, playableIds = emptyList()))
    }

    @Test
    fun tappingAMissingSongDoesNotStartAnotherTrack() {
        val start = playlistQueueStart(
            ids = listOf("a", "b", "c"),
            tappedIndex = 1,
            playableIds = listOf("a", "c"),
        )
        assertEquals(-1, start)
    }

    @Test
    fun tappingAnAvailableSongUsesItsPlaceInThePlayableQueue() {
        val start = playlistQueueStart(
            ids = listOf("a", "b", "c"),
            tappedIndex = 2,
            playableIds = listOf("a", "c"),
        )
        assertEquals(1, start)
    }
}
