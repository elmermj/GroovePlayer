package com.aethelsoft.grooveplayer.domain.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EmbeddedArtworkCacheTest {

    private val hash = "ab".repeat(32)
    private val other = "cd".repeat(32)

    @Test
    fun cacheKeyChangesWhenContentHashChanges() {
        val first = EmbeddedArtworkKeys.cacheKey(hash, 128, 128)
        val second = EmbeddedArtworkKeys.cacheKey(other, 128, 128)
        assertEquals("$hash@128", first)
        assertEquals("$other@128", second)
        assertFalse(first == second)
    }

    @Test
    fun cacheKeyFollowsDisplayEdgeAndCapsIt() {
        assertEquals("$hash@256", EmbeddedArtworkKeys.cacheKey(hash.uppercase(), 200, 256))
        assertEquals(
            "$hash@${EmbeddedArtworkKeys.MAX_EDGE_PX}",
            EmbeddedArtworkKeys.cacheKey(hash, 4000, 100),
        )
        assertEquals(
            "$hash@${EmbeddedArtworkKeys.DEFAULT_EDGE_PX}",
            EmbeddedArtworkKeys.cacheKey(hash, 0, 0),
        )
    }

    @Test
    fun uriRoundTripsOnlyForSha256() {
        val model = EmbeddedArtworkKeys.uri("  $hash  ")
        assertEquals("groove-artwork:$hash", model)
        assertEquals(hash, EmbeddedArtworkKeys.hashOf(model))
        assertEquals(hash, EmbeddedArtworkKeys.hashOf("groove-artwork://$hash"))
        assertNull(EmbeddedArtworkKeys.uri("nope"))
        assertNull(EmbeddedArtworkKeys.cacheKey("content://media/albumart/1", 64, 64))
    }

    @Test
    fun preferReplacesMediaStoreArtAndFollowsANewHash() {
        val model = EmbeddedArtworkKeys.uri(hash)!!
        val next = EmbeddedArtworkKeys.uri(other)!!
        assertEquals(model, EmbeddedArtworkKeys.prefer(null, model))
        assertEquals(model, EmbeddedArtworkKeys.prefer("content://media/external/audio/albumart/3", model))
        assertEquals(next, EmbeddedArtworkKeys.prefer(model, next))
        assertEquals("file:///covers/custom.jpg", EmbeddedArtworkKeys.prefer("file:///covers/custom.jpg", model))
        assertEquals("file:///covers/custom.jpg", EmbeddedArtworkKeys.displayUrl(null, "file:///covers/custom.jpg"))
    }

    @Test
    fun sampleSizeStaysOneUntilTheBitmapExceedsTheEdge() {
        assertEquals(1, EmbeddedArtworkKeys.sampleSize(400, 400, 512))
        assertEquals(2, EmbeddedArtworkKeys.sampleSize(1000, 800, 400))
        assertEquals(4, EmbeddedArtworkKeys.sampleSize(2000, 2000, 400))
    }

    @Test
    fun memoryAndDiskReuseBytesUntilTheHashChanges() {
        val dir = tempDir()
        val cache = EmbeddedArtworkCache(dir, memoryCapacity = 2)
        val small = EmbeddedArtworkKeys.cacheKey(hash, 64, 64)!!
        val large = EmbeddedArtworkKeys.cacheKey(hash, 256, 256)!!
        val otherKey = EmbeddedArtworkKeys.cacheKey(other, 64, 64)!!
        cache.write(small, byteArrayOf(1, 2, 3))
        cache.write(large, byteArrayOf(4, 5))
        cache.write(otherKey, byteArrayOf(9))

        assertTrue(cache.read(small)!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(cache.read(large)!!.contentEquals(byteArrayOf(4, 5)))
        assertFalse(cache.read(small)!!.contentEquals(cache.read(otherKey)!!))

        val reopened = EmbeddedArtworkCache(dir, memoryCapacity = 2)
        assertTrue(reopened.read(small)!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(reopened.read(otherKey)!!.contentEquals(byteArrayOf(9)))
        assertNull(reopened.read(EmbeddedArtworkKeys.cacheKey("e".repeat(64), 64, 64)!!))
    }

    @Test
    fun folderCoverPrefersCoverJpgThenFolderJpg() {
        val dir = tempDir()
        File(dir, "Folder.JPG").writeBytes(byteArrayOf(2))
        assertEquals("Folder.JPG", FolderArtwork.find(dir)!!.name)
        File(dir, "cover.jpg").writeBytes(byteArrayOf(1))
        assertEquals("cover.jpg", FolderArtwork.find(dir)!!.name)
        File(dir, "cover.jpg").writeBytes(byteArrayOf())
        File(dir, "notes.txt").writeBytes(byteArrayOf(3))
        assertEquals("Folder.JPG", FolderArtwork.find(dir)!!.name)
    }

    private fun tempDir(): File {
        return File(System.getProperty("java.io.tmpdir"), "groove-art-${System.nanoTime()}").apply {
            mkdirs()
            deleteOnExit()
        }
    }
}
