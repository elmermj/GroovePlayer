package com.aethelworks.grooveplayer.domain.transfer

import com.aethelworks.grooveplayer.data.transfer.TransferProtocol
import com.aethelworks.grooveplayer.domain.backup.AppPrivateLibrary
import com.aethelworks.grooveplayer.domain.backup.ContentHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReceivedTransferIntakeTest {

    @Test
    fun matchingHashIsKeptUnderGrooveLibraryAndCatalogued() {
        val parent = tempDir()
        val root = libraryRoot(parent)
        val staged = stage(root, "live.flac", byteArrayOf(1, 2, 3, 4, 5, 9))
        val hash = ContentHash.sha256(staged)
        val result = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = "live.flac",
            expectedSize = staged.length(),
            expectedChecksum = hash.uppercase(),
            knownPrivateHashes = emptySet(),
        )
        val catalog = catalogOf(result, root)
        assertEquals(listOf(hash), catalog)
        assertTrue(result.addCatalogRow)
        val stored = File(result.libraryPath!!)
        assertEquals(hash, ContentHash.sha256(stored))
        assertEquals("flac", stored.extension)
        assertEquals(root.absolutePath, stored.parentFile!!.absolutePath)
        assertFalse(staged.exists())
        assertTrue(incomingFiles(root).isEmpty())
    }

    @Test
    fun checksumMismatchDeletesStagingAndAddsNothingToCatalog() {
        val root = libraryRoot()
        val staged = stage(root, "bad.flac", "not-the-song".toByteArray())
        val result = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = "bad.flac",
            expectedSize = staged.length(),
            expectedChecksum = "ab".repeat(32),
            knownPrivateHashes = emptySet(),
        )
        assertEquals(ReceiveFailure.CHECKSUM_MISMATCH, result.failure)
        assertFalse(result.addCatalogRow)
        assertTrue(catalogOf(result, root).isEmpty())
        assertFalse(staged.exists())
        assertTrue(libraryAudio(root).isEmpty())
        assertTrue(incomingFiles(root).isEmpty())
    }

    @Test
    fun truncatedBytesWithTheDeclaredSizeAreRejected() {
        val root = libraryRoot()
        val original = ByteArray(32) { it.toByte() }
        val originalFile = stage(root, "full.wav", original)
        val hash = ContentHash.sha256(originalFile)
        originalFile.delete()
        val corrupted = original.copyOf()
        corrupted[0] = (corrupted[0] + 1).toByte()
        assertEquals(original.size, corrupted.size)
        val staged = stage(root, "full.wav", corrupted)
        val result = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = "full.wav",
            expectedSize = corrupted.size.toLong(),
            expectedChecksum = hash,
            knownPrivateHashes = emptySet(),
        )
        assertEquals(ReceiveFailure.CHECKSUM_MISMATCH, result.failure)
        assertTrue(catalogOf(result, root).isEmpty())
        assertFalse(staged.exists())
        assertTrue(libraryAudio(root).isEmpty())
    }

    @Test
    fun shortFileIsASizeMismatchAndIsDeleted() {
        val root = libraryRoot()
        val full = ByteArray(32) { 3 }
        val hashed = stage(root, "full.mp3", full)
        val hash = ContentHash.sha256(hashed)
        hashed.delete()
        val staged = stage(root, "full.mp3", full.copyOf(10))
        val result = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = "full.mp3",
            expectedSize = full.size.toLong(),
            expectedChecksum = hash,
            knownPrivateHashes = emptySet(),
        )
        assertEquals(ReceiveFailure.SIZE_MISMATCH, result.failure)
        assertTrue(catalogOf(result, root).isEmpty())
        assertFalse(staged.exists())
        assertTrue(libraryAudio(root).isEmpty())
    }

    @Test
    fun sizeLimitRejectionDeletesTheFileAndSkipsCatalog() {
        assertEquals(1024L * 1024L * 1024L, ReceivedTransferIntake.MAX_FILE_BYTES)
        val root = libraryRoot()
        val staged = stage(root, "huge.mp3", byteArrayOf(1, 2, 3, 4))
        val hash = ContentHash.sha256(staged)
        val overClaim = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = "huge.mp3",
            expectedSize = ReceivedTransferIntake.MAX_FILE_BYTES + 1,
            expectedChecksum = hash,
            knownPrivateHashes = emptySet(),
        )
        assertEquals(ReceiveFailure.OVER_LIMIT, overClaim.failure)
        assertTrue(catalogOf(overClaim, root).isEmpty())
        assertFalse(staged.exists())

        val tooBig = stage(root, "wide.ogg", ByteArray(16) { 7 })
        val limited = ReceivedTransferIntake.acceptStaged(
            staged = tooBig,
            libraryRoot = root,
            displayName = "wide.ogg",
            expectedSize = tooBig.length(),
            expectedChecksum = ContentHash.sha256(tooBig),
            knownPrivateHashes = emptySet(),
            maxBytes = 8L,
        )
        assertEquals(ReceiveFailure.OVER_LIMIT, limited.failure)
        assertTrue(catalogOf(limited, root).isEmpty())
        assertFalse(tooBig.exists())
        assertTrue(libraryAudio(root).isEmpty())
    }

    @Test
    fun senderExtensionIsPreserved() {
        val name = ReceivedTransferIntake.displayName("concert.flac", "Concert", "audio/mpeg")
        assertEquals("concert.flac", name)
        val root = libraryRoot()
        val bytes = "flac-bytes".toByteArray()
        val staged = stage(root, "concert.flac.partial", bytes)
        val result = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = name,
            expectedSize = bytes.size.toLong(),
            expectedChecksum = ContentHash.sha256(staged),
            knownPrivateHashes = emptySet(),
        )
        val stored = File(result.libraryPath!!)
        assertEquals("flac", stored.extension)
        assertFalse(stored.name.endsWith(".mp3"))
        assertEquals(bytes.toList(), stored.readBytes().toList())
    }

    @Test
    fun receivedFileLandsOnlyUnderThePrivateGrooveLibrary() {
        val parent = tempDir()
        val root = libraryRoot(parent)
        val staged = stage(root, "only.ogg", "ogg-bytes".toByteArray())
        val result = ReceivedTransferIntake.acceptStaged(
            staged = staged,
            libraryRoot = root,
            displayName = "only.ogg",
            expectedSize = staged.length(),
            expectedChecksum = ContentHash.sha256(staged),
            knownPrivateHashes = emptySet(),
        )
        val stored = File(result.libraryPath!!)
        assertEquals(AppPrivateLibrary.FOLDER_NAME, stored.parentFile!!.name)
        assertTrue(AppPrivateLibrary.isInside(stored.absolutePath, root.absolutePath))
        assertFalse(stored.absolutePath.contains("${File.separator}.incoming${File.separator}"))
        assertTrue(parent.listFiles()!!.none { it.isFile })
        assertEquals(listOf(stored.absolutePath), libraryAudio(root).map { it.absolutePath })

        val shared = File(tempDir(), "Music").apply { mkdirs() }
        val outside = File(shared, "shared.mp3")
        outside.writeBytes(byteArrayOf(8, 8, 8, 8))
        val rejected = ReceivedTransferIntake.acceptStaged(
            staged = outside,
            libraryRoot = shared,
            displayName = "shared.mp3",
            expectedSize = outside.length(),
            expectedChecksum = ContentHash.sha256(outside),
            knownPrivateHashes = emptySet(),
        )
        assertEquals(ReceiveFailure.OUTSIDE_LIBRARY, rejected.failure)
        assertTrue(catalogOf(rejected, shared).isEmpty())
        assertFalse(outside.exists())
        assertTrue(shared.listFiles().orEmpty().none { it.isFile })
    }

    @Test
    fun duplicateContentHashIsNotAddedToTheCatalog() {
        val root = libraryRoot()
        val bytes = byteArrayOf(4, 5, 6, 7, 8)
        val firstStaged = stage(root, "one.mp3", bytes)
        val hash = ContentHash.sha256(firstStaged)
        val first = ReceivedTransferIntake.acceptStaged(
            staged = firstStaged,
            libraryRoot = root,
            displayName = "one.mp3",
            expectedSize = bytes.size.toLong(),
            expectedChecksum = hash,
            knownPrivateHashes = emptySet(),
        )
        assertEquals(listOf(hash), catalogOf(first, root))
        val second = stage(root, "copy.mp3", bytes)
        val again = ReceivedTransferIntake.acceptStaged(
            staged = second,
            libraryRoot = root,
            displayName = "copy.mp3",
            expectedSize = bytes.size.toLong(),
            expectedChecksum = hash,
            knownPrivateHashes = setOf(hash.uppercase()),
        )
        assertTrue(again.alreadyInLibrary)
        assertFalse(again.addCatalogRow)
        assertTrue(catalogOf(again, root).isEmpty())
        assertFalse(second.exists())
        assertEquals(1, libraryAudio(root).size)
    }

    @Test
    fun senderComputesSha256AndSendsItWithFileMetadata() {
        val file = File(tempDir(), "live.flac")
        val bytes = "nearby-payload".toByteArray()
        file.writeBytes(bytes)
        val meta = TransferProtocol.metadataFor(file)
        assertEquals(ContentHash.sha256(file), meta.checksum)
        assertEquals(file.length(), meta.sizeBytes)
        val payload = TransferProtocol.encodeFileMetadata(listOf(meta))
        assertEquals(TransferProtocol.MSG_FILE_METADATA, payload[0].toInt() and 0xFF)
        val decoded = TransferProtocol.decodeFileMetadata(payload)
        assertEquals(meta.checksum, decoded!!.files.single().checksum)
        assertEquals("live.flac", decoded.files.single().name)
        assertTrue(String(payload, Charsets.ISO_8859_1).contains(meta.checksum))
    }

    @Test
    fun metadataMissingAChecksumIsNotAccepted() {
        val name = "song.mp3".toByteArray(Charsets.UTF_8)
        val message = ByteArray(13 + 2 + name.size + 8)
        message[0] = TransferProtocol.MSG_FILE_METADATA.toByte()
        ByteBuffer.wrap(message, 1, 8).order(ByteOrder.BIG_ENDIAN).putLong(4L)
        ByteBuffer.wrap(message, 9, 4).order(ByteOrder.BIG_ENDIAN).putInt(1)
        var pos = 13
        ByteBuffer.wrap(message, pos, 2).order(ByteOrder.BIG_ENDIAN).putShort(name.size.toShort())
        pos += 2
        System.arraycopy(name, 0, message, pos, name.size)
        pos += name.size
        ByteBuffer.wrap(message, pos, 8).order(ByteOrder.BIG_ENDIAN).putLong(4L)
        assertNull(TransferProtocol.decodeFileMetadata(message))
    }

    private fun catalogOf(result: AcceptResult, root: File): List<String> {
        val row = ReceivedTransferIntake.catalogRowOrNull(result, root)
        return if (row == null) emptyList() else listOf(row)
    }

    private fun libraryRoot(parent: File = tempDir()): File =
        File(parent, AppPrivateLibrary.FOLDER_NAME).apply { mkdirs() }

    private fun stage(root: File, name: String, bytes: ByteArray): File {
        val incoming = File(root, ".incoming").apply { mkdirs() }
        return File(incoming, name).apply { writeBytes(bytes) }
    }

    private fun libraryAudio(root: File): List<File> =
        root.listFiles().orEmpty().filter { it.isFile }

    private fun incomingFiles(root: File): List<File> =
        File(root, ".incoming").listFiles().orEmpty().filter { it.isFile }.toList()

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "groove-receive-${System.nanoTime()}").apply { mkdirs() }
}
