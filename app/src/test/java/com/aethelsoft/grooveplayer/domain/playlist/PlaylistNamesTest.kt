package com.aethelsoft.grooveplayer.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistNamesTest {

    @Test
    fun `names are trimmed and collapsed`() {
        assertEquals("Evening mix", PlaylistNames.sanitize("  Evening   mix \n"))
        assertNull(PlaylistNames.validationError("Evening mix"))
        assertEquals("Playlist name cannot be empty", PlaylistNames.validationError("   "))
    }

    @Test
    fun `display names drop the extension and export names are safe`() {
        assertEquals("Evening mix", PlaylistNames.fromDisplayName("Evening mix.m3u"))
        assertEquals("My_mix.m3u", PlaylistNames.fileName("My/mix"))
        assertEquals("playlist.m3u", PlaylistNames.fileName("   "))
    }
}
