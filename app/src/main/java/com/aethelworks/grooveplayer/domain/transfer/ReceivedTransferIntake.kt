package com.aethelworks.grooveplayer.domain.transfer

import com.aethelworks.grooveplayer.domain.backup.AppPrivateLibrary
import com.aethelworks.grooveplayer.domain.backup.ContentHash
import com.aethelworks.grooveplayer.domain.backup.GrooveDownloadPlacement
import com.aethelworks.grooveplayer.domain.library.LibraryFilePlacement
import com.aethelworks.grooveplayer.domain.library.PrivateLibrarySongs
import java.io.File

/**
 * Rules for a file that arrived over Nearby Connections (or the Wi-Fi share
 * socket that Nearby pairing starts). Bytes stay in the private library's
 * incoming directory until size and SHA-256 both pass. Only then does the file
 * move to the groove-library root, where the catalog can see it.
 *
 * Hashing goes through [ContentHash], the same helper library import uses.
 */
object ReceivedTransferIntake {
    /**
     * Largest single audio file a receive will keep. A longer claim or a larger
     * staged file is deleted and is not catalogued. 1 GiB still fits a long
     * lossless recording.
     */
    const val MAX_FILE_BYTES: Long = 1024L * 1024L * 1024L

    fun sizeFailure(
        staged: File,
        expectedSize: Long,
        maxBytes: Long = MAX_FILE_BYTES,
    ): ReceiveFailure? {
        if (expectedSize > maxBytes || (staged.isFile && staged.length() > maxBytes)) {
            return ReceiveFailure.OVER_LIMIT
        }
        if (!staged.isFile || expectedSize <= 0L || staged.length() != expectedSize) {
            return ReceiveFailure.SIZE_MISMATCH
        }
        return null
    }

    /** SHA-256 of [file] via [ContentHash], compared to the sender's checksum. */
    fun checksumMatches(file: File, expectedChecksum: String): Boolean {
        if (!file.isFile || expectedChecksum.isBlank()) return false
        return ContentHash.sha256(file).equals(expectedChecksum, ignoreCase = true)
    }

    /**
     * Size, hash, dedupe, then move into [libraryRoot]. Any failure deletes [staged]
     * and returns [AcceptResult.addCatalogRow] false.
     */
    fun acceptStaged(
        staged: File,
        libraryRoot: File,
        displayName: String,
        expectedSize: Long,
        expectedChecksum: String?,
        knownPrivateHashes: Set<String>,
        maxBytes: Long = MAX_FILE_BYTES,
    ): AcceptResult {
        val sizeProblem = sizeFailure(staged, expectedSize, maxBytes)
        if (sizeProblem != null) return rejectUnverified(staged, sizeProblem)
        val expected = expectedChecksum?.trim().orEmpty()
        if (!checksumMatches(staged, expected)) {
            return rejectUnverified(staged, ReceiveFailure.CHECKSUM_MISMATCH)
        }
        return acceptVerified(
            staged = staged,
            libraryRoot = libraryRoot,
            displayName = displayName,
            expectedSize = expectedSize,
            contentHash = expected,
            knownPrivateHashes = knownPrivateHashes,
        )
    }

    /**
     * Place a file whose checksum already matched. Does not hash again.
     * A duplicate of a private-library hash is deleted and not catalogued.
     */
    fun acceptVerified(
        staged: File,
        libraryRoot: File,
        displayName: String,
        expectedSize: Long,
        contentHash: String,
        knownPrivateHashes: Set<String>,
    ): AcceptResult {
        val sizeProblem = sizeFailure(staged, expectedSize)
        if (sizeProblem != null) return rejectUnverified(staged, sizeProblem)
        val hash = contentHash.trim().lowercase()
        if (hash.isEmpty()) return rejectUnverified(staged, ReceiveFailure.CHECKSUM_MISMATCH)
        if (hash in knownPrivateHashes.map { it.lowercase() }) {
            discard(staged)
            return AcceptResult(
                addCatalogRow = false,
                alreadyInLibrary = true,
                contentHash = hash,
            )
        }
        val root = libraryRoot.absoluteFile
        if (root.name != AppPrivateLibrary.FOLDER_NAME) {
            return rejectUnverified(staged, ReceiveFailure.OUTSIDE_LIBRARY)
        }
        val safeName = displayName(displayName, displayName, null)
        val dest = PrivateLibrarySongs.destination(root, safeName)
        if (!isStoredInPrivateLibrary(dest.absolutePath, root)) {
            return rejectUnverified(staged, ReceiveFailure.OUTSIDE_LIBRARY)
        }
        if (extensionOf(dest.name) != extensionOf(safeName)) {
            return rejectUnverified(staged, ReceiveFailure.OUTSIDE_LIBRARY)
        }
        root.mkdirs()
        val placed = LibraryFilePlacement.placeVerified(staged, dest, expectedSize)
        if (!placed || !isStoredInPrivateLibrary(dest.absolutePath, root)) {
            discard(staged)
            if (dest.exists() && dest.length() != expectedSize) dest.delete()
            return AcceptResult(addCatalogRow = false, failure = ReceiveFailure.UNREADABLE)
        }
        return AcceptResult(
            addCatalogRow = true,
            libraryPath = dest.absolutePath,
            contentHash = hash,
        )
    }

    fun rejectUnverified(staged: File, failure: ReceiveFailure): AcceptResult {
        discard(staged)
        return AcceptResult(addCatalogRow = false, failure = failure)
    }

    /**
     * Hash to insert, or null when this receive must not touch the catalog.
     * The file has to sit in the groove-library root, outside `.incoming`.
     */
    fun catalogRowOrNull(result: AcceptResult, libraryRoot: File): String? {
        if (!result.addCatalogRow || result.alreadyInLibrary) return null
        val path = result.libraryPath ?: return null
        val hash = result.contentHash?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        if (!isStoredInPrivateLibrary(path, libraryRoot)) return null
        if (!File(path).isFile) return null
        return hash
    }

    fun isStoredInPrivateLibrary(path: String, libraryRoot: File): Boolean {
        val root = libraryRoot.absoluteFile
        if (root.name != AppPrivateLibrary.FOLDER_NAME) return false
        val file = File(path).absoluteFile
        if (!AppPrivateLibrary.isInside(file.path, root.path)) return false
        if (file.parentFile?.absolutePath != root.absolutePath) return false
        val incoming = File(root, PrivateLibrarySongs.INCOMING_DIR)
        if (AppPrivateLibrary.isInside(file.path, incoming.absolutePath)) return false
        return PrivateLibrarySongs.isListedAudio(file.name)
    }

    /**
     * Cosmetic name for the library file. Keeps the sender's extension.
     * A missing name falls back to [mimeType], never a hard-coded `.mp3`.
     */
    fun displayName(sentFileName: String?, title: String, mimeType: String?): String {
        val fromSender = sentFileName?.trim().orEmpty()
        val raw = if (fromSender.isNotEmpty() && extensionOf(fromSender).isNotEmpty()) {
            fromSender
        } else {
            val ext = extensionFromMime(mimeType).ifEmpty { extensionOf(fromSender) }
            val stemSource = when {
                fromSender.isNotEmpty() -> fromSender.substringBeforeLast('.', fromSender)
                else -> title
            }
            val stem = stemSource.trim().ifBlank { "audio" }
            if (ext.isNotEmpty()) "$stem.$ext" else stem
        }
        return GrooveDownloadPlacement.sanitizeFileName(raw, "audio")
    }

    fun extensionOf(name: String): String {
        val ext = name.substringAfterLast('.', "")
        val usable = ext.isNotEmpty() && ext != name && ext.length <= 8 && ext.all { it.isLetterOrDigit() }
        return if (usable) ext.lowercase() else ""
    }

    private fun extensionFromMime(mimeType: String?): String {
        return when (mimeType?.substringBefore(';')?.trim()?.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/aac", "audio/aacp" -> "aac"
            "audio/ogg", "audio/vorbis" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            else -> ""
        }
    }

    private fun discard(file: File) {
        if (file.exists()) file.delete()
    }
}

enum class ReceiveFailure {
    OVER_LIMIT,
    SIZE_MISMATCH,
    CHECKSUM_MISMATCH,
    UNREADABLE,
    OUTSIDE_LIBRARY,
}

fun ReceiveFailure.userMessage(): String = when (this) {
    ReceiveFailure.OVER_LIMIT -> "File is too large"
    ReceiveFailure.SIZE_MISMATCH -> "File size did not match"
    ReceiveFailure.CHECKSUM_MISMATCH -> "Checksum did not match"
    ReceiveFailure.UNREADABLE -> "Received file could not be stored"
    ReceiveFailure.OUTSIDE_LIBRARY -> "Received file was outside the private library"
}

data class AcceptResult(
    val addCatalogRow: Boolean,
    val alreadyInLibrary: Boolean = false,
    val libraryPath: String? = null,
    val contentHash: String? = null,
    val failure: ReceiveFailure? = null,
)
