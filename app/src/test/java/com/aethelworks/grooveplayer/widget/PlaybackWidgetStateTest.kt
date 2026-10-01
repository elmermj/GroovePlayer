package com.aethelworks.grooveplayer.widget

import com.aethelworks.grooveplayer.domain.artwork.EmbeddedArtworkKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaybackWidgetStateTest {

    private val hash = "ab".repeat(32)

    @Test
    fun emptyQueueHasNoTrackAndIsNotPlaying() {
        val state = PlaybackWidgetState.from(
            trackId = "song-1",
            title = "   ",
            artist = "Someone",
            isPlaying = true,
            artworkUrl = "https://cdn.example/art.jpg",
        )
        assertEquals(PlaybackWidgetState.EMPTY, state)
        assertFalse(state.hasTrack)
        assertFalse(state.isPlaying)
        assertFalse(transportControlsActive(state))
        assertNull(state.artworkUrl)
    }

    @Test
    fun nullSongIsTheEmptyState() {
        val state = PlaybackWidgetState.from(
            trackId = null,
            title = null,
            artist = null,
            isPlaying = false,
            artworkUrl = null,
        )
        assertFalse(state.hasTrack)
        assertEquals("", state.title)
        assertEquals("", state.artist)
    }

    @Test
    fun queuedTrackKeepsTitleArtistAndPlayState() {
        val playing = PlaybackWidgetState.from(
            trackId = " song-1 ",
            title = "  Night Drive ",
            artist = "  Ada ",
            isPlaying = true,
            artworkUrl = " content://media/external/audio/albumart/3 ",
        )
        assertTrue(playing.hasTrack)
        assertTrue(transportControlsActive(playing))
        assertEquals("song-1", playing.trackId)
        assertEquals("Night Drive", playing.title)
        assertEquals("Ada", playing.artist)
        assertTrue(playing.isPlaying)
        assertEquals("content://media/external/audio/albumart/3", playing.artworkUrl)

        val paused = playing.copy(isPlaying = false)
        assertTrue(paused.hasTrack)
        assertFalse(paused.isPlaying)
    }

    @Test
    fun remoteArtworkIsNotALocalSource() {
        val cache = tempDir()
        try {
            File(cache, "cover.jpg").writeBytes(byteArrayOf(1, 2, 3))
            assertNull(localArtworkUri("https://cdn.example/art.jpg"))
            assertNull(localArtworkUri("http://cdn.example/art.jpg"))
            assertNull(localArtworkUri("groove-artwork:$hash"))
            assertNull(localArtworkUri("/storage/emulated/0/cover.jpg"))
            assertEquals(WidgetArtworkSource.None, widgetArtworkSource(cache, "https://cdn.example/art.jpg"))
            assertEquals(WidgetArtworkSource.None, widgetArtworkSource(cache, null))
            assertEquals(WidgetArtworkSource.None, widgetArtworkSource(cache, "  "))
        } finally {
            cache.deleteRecursively()
        }
    }

    @Test
    fun contentAndFileUrisAreLocalSources() {
        val cache = tempDir()
        try {
            val content = "content://media/external/audio/albumart/9"
            val file = "file:///data/user/0/cover.jpg"
            assertEquals(
                "CONTENT://media/external/audio/albumart/9",
                localArtworkUri("CONTENT://media/external/audio/albumart/9"),
            )
            assertEquals(content, localArtworkUri(content))
            assertEquals(file, localArtworkUri(file))
            assertEquals(
                WidgetArtworkSource.LocalUri(content),
                widgetArtworkSource(cache, content),
            )
            assertEquals(
                WidgetArtworkSource.LocalUri(file),
                widgetArtworkSource(cache, file),
            )
        } finally {
            cache.deleteRecursively()
        }
    }

    @Test
    fun cachedEmbeddedArtworkIsUsedWhenTheFileIsAlreadyThere() {
        val cache = tempDir()
        try {
            val key = EmbeddedArtworkKeys.cacheKey(hash, 128, 128)!!
            val file = File(cache, EmbeddedArtworkKeys.fileName(key))
            file.writeBytes(byteArrayOf(1, 2, 3))
            val empty = File(cache, EmbeddedArtworkKeys.fileName(EmbeddedArtworkKeys.cacheKey(hash, 64, 64)!!))
            empty.writeBytes(byteArrayOf())

            val source = widgetArtworkSource(cache, "groove-artwork:${hash.uppercase()}")
            assertTrue(source is WidgetArtworkSource.CacheFile)
            assertEquals(file, (source as WidgetArtworkSource.CacheFile).file)

            val other = "cd".repeat(32)
            assertEquals(WidgetArtworkSource.None, widgetArtworkSource(cache, "groove-artwork:$other"))
        } finally {
            cache.deleteRecursively()
        }
    }

    private fun tempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "widget-art-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
