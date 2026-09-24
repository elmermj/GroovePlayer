package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.data.backup.GrooveLibraryWriteProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppPrivateLibraryTest {

    @Test
    fun privateCandidatesNeverUseSharedMusic() {
        val external = File("/storage/emulated/0/Android/data/com.aethelsoft.grooveplayer/files")
        val internal = File("/data/user/0/com.aethelsoft.grooveplayer/files")
        val publicMusic = File("/storage/emulated/0/Music")
        val candidates = AppPrivateLibrary.privateCandidates(external, internal)
        assertEquals(File(external, "groove-library"), candidates[0])
        assertEquals(File(internal, "groove-library"), candidates[1])
        assertTrue(candidates.none { it.path.contains("/Music/") })
        assertTrue(candidates.none { AppPrivateLibrary.isInside(it.path, publicMusic.path) })
    }

    @Test
    fun failedExternalCreateFallsThroughToInternalFiles() {
        val external = File("/ext/groove-library")
        val internal = File("/int/groove-library")
        val chosen = AppPrivateLibrary.firstCreatable(listOf(external, internal)) { dir ->
            dir == internal
        }
        assertEquals(internal, chosen)
    }

    @Test
    fun legacySharedMusicIsNotDeletedAndIsNotAPrivateRoot() {
        val publicMusic = File("/storage/emulated/0/Music")
        val external = File("/storage/emulated/0/Android/data/pkg/files")
        val internal = File("/data/user/0/pkg/files")
        val legacy = AppPrivateLibrary.legacyCandidates(publicMusic, external, internal)
        val shared = legacy.first()
        assertEquals(File(publicMusic, "Groove Downloads"), shared.directory)
        assertFalse(shared.deleteAfterCopy)
        assertTrue(legacy.drop(1).all { it.deleteAfterCopy })
        assertTrue(
            AppPrivateLibrary.privateCandidates(external, internal).none { candidate ->
                legacy.any { it.directory.path == candidate.path }
            },
        )
    }

    @Test
    fun adoptionCopiesReadableLeftoversAndKeepsSharedOriginals() {
        val root = tempDir()
        val shared = File(root, "Music/Groove Downloads").apply { mkdirs() }
        val owned = File(root, "old-app/Groove Downloads").apply { mkdirs() }
        val privateRoot = File(root, "groove-library").apply { mkdirs() }
        File(shared, "shared.mp3").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(shared, "partial.tmp").writeBytes(byteArrayOf(9))
        File(owned, "owned.mp3").writeBytes(byteArrayOf(5, 6))
        File(privateRoot, "owned.mp3").writeBytes(byteArrayOf(5, 6))

        val plan = LegacyLibraryAdoption.plan(
            legacyDirs = listOf(
                LegacyLibraryDir(shared, deleteAfterCopy = false),
                LegacyLibraryDir(owned, deleteAfterCopy = true),
            ),
            privateRoot = privateRoot,
        )
        val sharedCopy = plan.single { it.source.name == "shared.mp3" }
        assertTrue(sharedCopy.needsBytes)
        assertFalse(sharedCopy.deleteSourceAfterCopy)
        assertEquals(File(privateRoot, "shared.mp3"), sharedCopy.destination)
        assertTrue(plan.none { it.source.name.endsWith(".tmp") })

        val ownedCopy = plan.single { it.source.name == "owned.mp3" }
        assertFalse(ownedCopy.needsBytes)
        assertTrue(ownedCopy.deleteSourceAfterCopy)
        assertEquals(File(privateRoot, "owned.mp3"), ownedCopy.destination)
    }

    @Test
    fun adoptionRenamesWhenThePrivateNameHoldsDifferentBytes() {
        val root = tempDir()
        val legacy = File(root, "Groove Downloads").apply { mkdirs() }
        val privateRoot = File(root, "groove-library").apply { mkdirs() }
        File(legacy, "song.mp3").writeBytes(byteArrayOf(1, 2, 3))
        File(privateRoot, "song.mp3").writeBytes(byteArrayOf(9))

        val plan = LegacyLibraryAdoption.plan(
            listOf(LegacyLibraryDir(legacy, deleteAfterCopy = false)),
            privateRoot,
        )
        assertEquals(1, plan.size)
        assertTrue(plan.single().needsBytes)
        assertEquals("song__3.mp3", plan.single().destination.name)
    }

    @Test
    fun writeProbeCreatesAndRemovesATempFile() {
        val dir = tempDir()
        assertTrue(GrooveLibraryWriteProbe.canCreateFile(dir))
        assertTrue(dir.list().isNullOrEmpty())
        val notDir = File(dir, "file")
        notDir.writeText("x")
        assertFalse(GrooveLibraryWriteProbe.canCreateFile(notDir))
    }

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "groove-lib-${System.nanoTime()}").apply { mkdirs() }
}
