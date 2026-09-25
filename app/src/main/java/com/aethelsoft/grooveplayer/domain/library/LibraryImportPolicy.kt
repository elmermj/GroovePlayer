package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.backup.AppPrivateLibrary
import com.aethelsoft.grooveplayer.domain.backup.ContentHash
import com.aethelsoft.grooveplayer.domain.backup.GrooveDownloadPlacement
import com.aethelsoft.grooveplayer.domain.model.Song
import java.io.File
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import java.security.MessageDigest

/** Audio the folder picker may copy into the private library. */
object LibraryAudioKinds {
    val EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav")

    fun isAudio(displayName: String, mimeType: String?): Boolean {
        if (displayName.startsWith(".")) return false
        if (AppPrivateLibrary.isScratchFile(displayName)) return false
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase()
        if (mime != null && mime.startsWith("audio/")) return true
        val ext = displayName.substringAfterLast('.', "")
        return ext.isNotEmpty() && ext != displayName && ext.lowercase() in EXTENSIONS
    }
}

enum class ImportFileStatus {
    COPIED,
    ALREADY_IN_LIBRARY,
    FAILED,
    CANCELLED,
}

data class ImportFileResult(
    val displayName: String,
    val stableKey: String,
    val status: ImportFileStatus,
    val sha256: String? = null,
    val sizeBytes: Long = 0L,
    val libraryPath: String? = null,
    /** True only when this import wrote a hash-verified library copy. */
    val verifiedCopy: Boolean = false,
    val failureReason: String? = null,
)

data class ImportRunResult(
    val files: List<ImportFileResult>,
    val stoppedReason: String? = null,
) {
    val copied: Int get() = files.count { it.status == ImportFileStatus.COPIED }
    val alreadyInLibrary: Int get() = files.count { it.status == ImportFileStatus.ALREADY_IN_LIBRARY }
    val failed: List<ImportFileResult> get() = files.filter { it.status == ImportFileStatus.FAILED }
    val cancelled: Boolean get() = files.any { it.status == ImportFileStatus.CANCELLED }
}

interface ImportByteSource {
    val displayName: String
    val sizeBytes: Long
    val stableKey: String
    fun open(): InputStream
}

object LibraryFileCopy {
    const val BUFFER_BYTES = 8 * 1024

    /**
     * Stream-copies [input] to [dest] while hashing the source bytes, then checks the
     * copy's size and SHA-256. A mismatch or cancel deletes [dest] and leaves nothing else.
     */
    fun copyAndVerify(
        input: InputStream,
        dest: File,
        expectedSize: Long,
        cancelled: () -> Boolean,
        onBytes: (Long) -> Unit = {},
    ): ImportFileResult {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        val digest = MessageDigest.getInstance("SHA-256")
        var written = 0L
        var stopped = false
        try {
            dest.outputStream().use { out ->
                val buf = ByteArray(BUFFER_BYTES)
                while (true) {
                    if (cancelled()) {
                        stopped = true
                        break
                    }
                    val read = input.read(buf)
                    if (read < 0) break
                    digest.update(buf, 0, read)
                    out.write(buf, 0, read)
                    written += read
                    onBytes(written)
                }
            }
        } catch (e: Exception) {
            if (dest.exists()) dest.delete()
            val reason = if (isOutOfSpace(e)) OUT_OF_SPACE else (e.message ?: "Copy failed")
            return ImportFileResult(
                displayName = dest.name,
                stableKey = "",
                status = ImportFileStatus.FAILED,
                failureReason = reason,
            )
        }
        if (stopped) {
            if (dest.exists()) dest.delete()
            return ImportFileResult(
                displayName = dest.name,
                stableKey = "",
                status = ImportFileStatus.CANCELLED,
            )
        }
        val sourceHash = digest.digest().joinToString("") { b -> "%02x".format(b) }
        val sizeOk = dest.isFile && dest.length() == written &&
            (expectedSize <= 0L || dest.length() == expectedSize)
        val destHash = if (sizeOk) ContentHash.sha256(dest) else ""
        if (!sizeOk || !destHash.equals(sourceHash, ignoreCase = true)) {
            if (dest.exists()) dest.delete()
            return ImportFileResult(
                displayName = dest.name,
                stableKey = "",
                status = ImportFileStatus.FAILED,
                failureReason = "Copied file did not match the original",
            )
        }
        return ImportFileResult(
            displayName = dest.name,
            stableKey = "",
            status = ImportFileStatus.COPIED,
            sha256 = sourceHash,
            sizeBytes = dest.length(),
            libraryPath = dest.absolutePath,
            verifiedCopy = true,
        )
    }

    /** Deletes [dest] unless its bytes still match [expectedHash] and [expectedSize]. */
    fun discardUnlessMatch(dest: File, expectedHash: String, expectedSize: Long): Boolean {
        val ok = dest.isFile &&
            dest.length() == expectedSize &&
            expectedSize > 0L &&
            ContentHash.sha256(dest).equals(expectedHash, ignoreCase = true)
        if (!ok && dest.exists()) dest.delete()
        return ok && dest.exists()
    }

    fun isOutOfSpace(error: Exception): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return message.contains("enospc") || message.contains("no space")
    }

    const val OUT_OF_SPACE = "Not enough free space"
}

object LibraryImporter {
    fun hasRoomFor(totalBytes: Long, freeBytes: Long): Boolean =
        totalBytes <= 0L || freeBytes >= totalBytes

    fun importAll(
        sources: List<ImportByteSource>,
        root: File,
        knownPrivateHashes: Set<String>,
        freeBytes: () -> Long,
        cancelled: () -> Boolean,
        onProgress: (completed: Int, total: Int, fileName: String) -> Unit = { _, _, _ -> },
    ): ImportRunResult {
        val incoming = File(root, PrivateLibrarySongs.INCOMING_DIR)
        incoming.mkdirs()
        val known = knownPrivateHashes.map { it.lowercase() }.toMutableSet()
        val results = mutableListOf<ImportFileResult>()
        val total = sources.size
        if (cancelled()) {
            return ImportRunResult(emptyList(), stoppedReason = null)
        }
        val needed = sources.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        if (!hasRoomFor(needed, freeBytes())) {
            return ImportRunResult(
                files = emptyList(),
                stoppedReason = LibraryFileCopy.OUT_OF_SPACE,
            )
        }
        for ((index, source) in sources.withIndex()) {
            if (cancelled()) {
                results += ImportFileResult(
                    displayName = source.displayName,
                    stableKey = source.stableKey,
                    status = ImportFileStatus.CANCELLED,
                )
                break
            }
            onProgress(index, total, source.displayName)
            if (source.sizeBytes > 0L && source.sizeBytes > freeBytes()) {
                results += ImportFileResult(
                    displayName = source.displayName,
                    stableKey = source.stableKey,
                    status = ImportFileStatus.FAILED,
                    failureReason = LibraryFileCopy.OUT_OF_SPACE,
                )
                return ImportRunResult(results, stoppedReason = LibraryFileCopy.OUT_OF_SPACE)
            }
            val partial = File(incoming, "${sanitize(source.displayName)}.partial")
            val copied = try {
                source.open().use { input ->
                    LibraryFileCopy.copyAndVerify(
                        input = input,
                        dest = partial,
                        expectedSize = source.sizeBytes,
                        cancelled = cancelled,
                    )
                }
            } catch (e: Exception) {
                if (partial.exists()) partial.delete()
                ImportFileResult(
                    displayName = source.displayName,
                    stableKey = source.stableKey,
                    status = ImportFileStatus.FAILED,
                    failureReason = e.message ?: "Could not read file",
                )
            }
            val finished = when (copied.status) {
                ImportFileStatus.CANCELLED -> {
                    if (partial.exists()) partial.delete()
                    copied.copy(displayName = source.displayName, stableKey = source.stableKey)
                }
                ImportFileStatus.FAILED -> copied.copy(
                    displayName = source.displayName,
                    stableKey = source.stableKey,
                )
                ImportFileStatus.COPIED -> {
                    val hash = copied.sha256!!.lowercase()
                    if (hash in known) {
                        partial.delete()
                        ImportFileResult(
                            displayName = source.displayName,
                            stableKey = source.stableKey,
                            status = ImportFileStatus.ALREADY_IN_LIBRARY,
                            sha256 = hash,
                            sizeBytes = copied.sizeBytes,
                        )
                    } else {
                        val dest = PrivateLibrarySongs.destination(root, source.displayName)
                        val moved = LibraryFilePlacement.placeVerified(partial, dest, copied.sizeBytes)
                        if (!moved) {
                            if (partial.exists()) partial.delete()
                            ImportFileResult(
                                displayName = source.displayName,
                                stableKey = source.stableKey,
                                status = ImportFileStatus.FAILED,
                                failureReason = "Could not place the copy in the library",
                            )
                        } else {
                            known += hash
                            ImportFileResult(
                                displayName = source.displayName,
                                stableKey = source.stableKey,
                                status = ImportFileStatus.COPIED,
                                sha256 = hash,
                                sizeBytes = copied.sizeBytes,
                                libraryPath = dest.absolutePath,
                                verifiedCopy = true,
                            )
                        }
                    }
                }
                ImportFileStatus.ALREADY_IN_LIBRARY -> copied
            }
            results += finished
            if (finished.status == ImportFileStatus.CANCELLED) break
            if (finished.failureReason == LibraryFileCopy.OUT_OF_SPACE) {
                return ImportRunResult(results, stoppedReason = LibraryFileCopy.OUT_OF_SPACE)
            }
        }
        onProgress(results.count { it.status != ImportFileStatus.CANCELLED }, total, "")
        return ImportRunResult(results)
    }

    private fun sanitize(name: String): String =
        GrooveDownloadPlacement.sanitizeFileName(name, "audio")
}

/** Songs shown in the catalog are private-library files that are still on disk. */
object PrivateCatalogFilter {
    fun listedPaths(
        paths: List<String>,
        privateRoot: String,
        fileExists: (String) -> Boolean,
    ): List<String> {
        return paths.filter { path ->
            GrooveDownloadPlacement.isInside(path, privateRoot) && fileExists(path)
        }
    }

    fun listedSongs(
        songs: List<Song>,
        privateRoot: String,
        fileExists: (String) -> Boolean,
    ): List<Song> {
        val keep = listedPaths(
            songs.mapNotNull { it.filePath },
            privateRoot,
            fileExists,
        ).toSet()
        return songs.filter { it.filePath != null && it.filePath in keep }
    }
}

/** Backup uploads catalog songs that already live in the private library. */
object BackupLibraryFiles {
    fun select(songs: List<Song>, isPrivateFile: (String) -> Boolean): List<String> {
        return songs.mapNotNull { song ->
            val path = song.filePath ?: return@mapNotNull null
            path.takeIf(isPrivateFile)
        }.distinct()
    }
}

data class SongHashRow(
    val songId: String,
    val contentHash: String?,
    val inPrivateLibrary: Boolean,
)

data class LinkedSongIds(
    val likes: Set<String> = emptySet(),
    val plays: List<String> = emptyList(),
    val metadata: Set<String> = emptySet(),
    val playlistMembers: Set<String> = emptySet(),
    val queue: List<String> = emptyList(),
    val lastPlayed: String? = null,
)

data class HashRemap(
    val canonicalSongId: String,
    val createdNewId: Boolean,
    val links: LinkedSongIds,
    val retiredSongIds: List<String>,
)

object SongHashRemap {
    fun privateHashes(rows: List<SongHashRow>): Set<String> {
        return rows.filter { it.inPrivateLibrary }
            .mapNotNull { it.contentHash?.lowercase()?.takeIf { hash -> hash.isNotBlank() } }
            .toSet()
    }

    /**
     * Point likes, play history, playlist membership, and the queue at the song
     * that owns [hash]. An existing private-library row wins. Otherwise the row
     * with the most plays (then a like) keeps its id. A brand-new file gets
     * `private:<hash>` and does not steal unrelated rows.
     */
    fun remap(hash: String, rows: List<SongHashRow>, links: LinkedSongIds): HashRemap {
        val key = hash.lowercase()
        val matches = rows.filter { it.contentHash?.equals(key, ignoreCase = true) == true }
        val privateMatch = matches.firstOrNull { it.inPrivateLibrary }
        val canonical = when {
            privateMatch != null -> privateMatch.songId
            matches.isNotEmpty() -> matches.maxWith(compareBy<SongHashRow> { score(it.songId, links) }
                .thenBy { it.songId }).songId
            else -> "private:$key"
        }
        val created = matches.isEmpty()
        val retired = matches.map { it.songId }.filter { it != canonical }
        val alias = retired.toSet()
        fun mapId(id: String) = if (id in alias) canonical else id
        val likes = links.likes.map(::mapId).toSet()
        val plays = links.plays.map(::mapId)
        val metadata = links.metadata.map(::mapId).toSet()
        val playlists = links.playlistMembers.map(::mapId).toSet()
        val queue = links.queue.map(::mapId)
        val last = links.lastPlayed?.let(::mapId)
        return HashRemap(
            canonicalSongId = canonical,
            createdNewId = created,
            links = LinkedSongIds(likes, plays, metadata, playlists, queue, last),
            retiredSongIds = retired,
        )
    }

    private fun score(songId: String, links: LinkedSongIds): Int {
        val plays = links.plays.count { it == songId }
        val like = if (songId in links.likes) 1_000 else 0
        return plays + like
    }
}

enum class OriginalDeleteChoice {
    KEEP,
    DELETE,
    DISMISSED,
}

object ImportOriginalDeletion {
    fun shouldPrompt(copied: Int, alreadyInLibrary: Int): Boolean =
        copied > 0 || alreadyInLibrary > 0

    fun eligibleKeys(files: List<ImportFileResult>): List<ImportFileResult> {
        return files.filter { it.status == ImportFileStatus.COPIED && it.verifiedCopy && !it.sha256.isNullOrBlank() }
    }

    /**
     * Keep is the default. Dismiss, back, and never answering delete nothing.
     * A hash that is no longer in the library is not eligible.
     */
    fun keysToDelete(
        choice: OriginalDeleteChoice,
        eligible: List<ImportFileResult>,
        libraryStillHasHash: (String) -> Boolean,
    ): List<ImportFileResult> {
        if (choice != OriginalDeleteChoice.DELETE) return emptyList()
        return eligible.filter { item ->
            val hash = item.sha256 ?: return@filter false
            item.verifiedCopy && libraryStillHasHash(hash)
        }
    }
}

object ImportPromptCopy {
    const val TITLE = "Import complete"
    const val KEEP = "Keep originals"
    const val DELETE = "Delete originals"

    fun body(copied: Int, folderName: String): String =
        "$copied songs were copied into GroovePlayer. Do you want to delete the original files from $folderName? Your copies in GroovePlayer stay."

    fun progress(completed: Int, total: Int): String = "Importing $completed of $total"

    fun deleteReport(deleted: Int, failed: Int): String {
        val total = deleted + failed
        return "Deleted $deleted of $total; $failed couldn't be deleted"
    }
}

object LibraryUpgradePrompt {
    const val BODY = "GroovePlayer now keeps its own library. Import your music folders to see your songs again."
    const val IMPORT = "Import folders"
    const val LATER = "Later"

    fun shouldShow(hiddenSongCount: Int, dismissed: Boolean): Boolean =
        hiddenSongCount > 0 && !dismissed
}

/** SQL for Room 17 → 18. Adds columns only; existing rows stay. */
object PrivateLibrarySchemaMigration {
    val STATEMENTS = listOf(
        "ALTER TABLE songs ADD COLUMN contentHash TEXT",
        "ALTER TABLE songs ADD COLUMN inPrivateLibrary INTEGER NOT NULL DEFAULT 0",
    )

    fun inPrivateLibrary(sourcePath: String?, privateRoot: String): Boolean {
        if (sourcePath.isNullOrBlank()) return false
        return GrooveDownloadPlacement.isInside(sourcePath, privateRoot)
    }
}

data class TrackPresence(
    val playable: Boolean,
    val canRestore: Boolean,
)

fun trackPresence(localFilePresent: Boolean, cloudCopyExists: Boolean): TrackPresence {
    return TrackPresence(
        playable = localFilePresent,
        canRestore = !localFilePresent && cloudCopyExists,
    )
}

/**
 * Moves a verified partial onto [dest]. A file that already exists at [dest] is never deleted.
 * Only a destination this call itself creates is removed when the size check fails.
 */
object LibraryFilePlacement {
    fun placeVerified(partial: File, dest: File, size: Long): Boolean {
        if (!partial.isFile || partial.length() != size) return false
        dest.parentFile?.mkdirs()
        if (dest.exists()) return false
        if (partial.renameTo(dest)) {
            if (dest.isFile && dest.length() == size) return true
            dest.delete()
            return false
        }
        if (dest.exists()) return false
        return try {
            partial.copyTo(dest, overwrite = false)
            if (dest.isFile && dest.length() == size) {
                partial.delete()
                true
            } else {
                dest.delete()
                false
            }
        } catch (_: FileAlreadyExistsException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

/** Name-and-size MediaStore hits. A row is deletable only when its folder matches the picked tree. */
data class MediaStoreAudioRow(
    val id: Long,
    val relativePath: String?,
    val contentHash: String? = null,
)

object MediaStoreOriginalMatch {
    fun uniqueId(
        rows: List<MediaStoreAudioRow>,
        pickedTreeDocumentId: String,
        importedSha256: String?,
    ): Long? {
        return rows.filter { isExact(it, pickedTreeDocumentId, importedSha256) }
            .singleOrNull()
            ?.id
    }

    fun isExact(
        row: MediaStoreAudioRow,
        pickedTreeDocumentId: String,
        importedSha256: String?,
    ): Boolean {
        if (!folderMatches(row.relativePath, pickedTreeDocumentId)) return false
        val want = importedSha256?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        val have = row.contentHash?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        return have.equals(want, ignoreCase = true)
    }

    fun folderMatches(relativePath: String?, pickedTreeDocumentId: String): Boolean {
        val picked = folderKey(pickedTreeDocumentId) ?: return false
        val folder = folderKey(relativePath) ?: return false
        return folder.equals(picked, ignoreCase = true) ||
            folder.startsWith("$picked/", ignoreCase = true)
    }

    fun folderKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val path = raw.substringAfter(':', raw).trim().trim('/')
        return path.ifBlank { null }
    }
}

fun folderDisplayName(treeDocumentId: String): String {
    val path = treeDocumentId.substringAfter(':', treeDocumentId).trim().trimEnd('/')
    val name = path.substringAfterLast('/').ifBlank { path }
    return name.ifBlank { "selected folder" }
}
