package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.backup.AppPrivateLibrary
import com.aethelsoft.grooveplayer.domain.backup.GrooveDownloadPlacement
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.makeAlbumId
import java.io.File

/**
 * Songs that live only in the app-private library.
 * They are not MediaStore rows and must not be placed under shared Music.
 */
object PrivateLibrarySongs {
    const val ID_PREFIX = "groove-library:"
    const val INCOMING_DIR = ".incoming"
    const val ARTIST = "Unknown Artist"
    const val ALBUM = "App library"

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "flac", "ogg", "wav", "aac", "opus", "wma",
    )

    fun isListedAudio(name: String): Boolean {
        if (name.startsWith(".")) return false
        if (AppPrivateLibrary.isScratchFile(name)) return false
        val ext = name.substringAfterLast('.', "")
        return ext.isNotEmpty() && ext != name && ext.lowercase() in AUDIO_EXTENSIONS
    }

    fun songId(absolutePath: String): String = "$ID_PREFIX$absolutePath"

    fun toSong(
        absolutePath: String,
        displayName: String,
        sizeBytes: Long,
        durationMs: Long = 0L,
        title: String = displayName.substringBeforeLast('.', displayName),
        artist: String = ARTIST,
    ): Song {
        val file = File(absolutePath)
        val albumArtist = artist.ifBlank { ARTIST }
        val albumName = ALBUM
        return Song(
            id = songId(absolutePath),
            title = title.ifBlank { displayName },
            artist = albumArtist,
            uri = file.toURI().toString(),
            genre = "",
            durationMs = durationMs.coerceAtLeast(0L),
            album = Album(
                id = makeAlbumId(albumArtist, albumName),
                name = albumName,
                artist = albumArtist,
                artworkUrl = null,
                songs = emptyList(),
            ),
            filePath = absolutePath,
            fileSizeBytes = sizeBytes.takeIf { it > 0L },
        )
    }

    /** Final file inside [root]. Never a shared Music path. A taken name gets a numeric suffix. */
    fun destination(root: File, displayName: String): File {
        val safe = GrooveDownloadPlacement.sanitizeFileName(displayName, "audio")
        val direct = File(root, safe)
        if (!direct.exists()) return direct
        val ext = safe.substringAfterLast('.', "")
        val usableExt = ext.isNotEmpty() && ext != safe && ext.length <= 8 && ext.all { it.isLetterOrDigit() }
        val stem = if (usableExt) safe.substringBeforeLast('.') else safe
        var index = 2
        while (true) {
            val name = if (usableExt) "${stem}__$index.$ext" else "${stem}__$index"
            val candidate = File(root, name)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    /**
     * Move a finished receive from staging into the library root.
     * Returns false when the staged bytes are missing; a failed copy does not keep a short library file.
     */
    fun promote(staged: File, dest: File): Boolean {
        if (!staged.isFile || staged.length() <= 0L) return false
        val size = staged.length()
        dest.parentFile?.mkdirs()
        if (staged.absolutePath == dest.absolutePath) return true
        if (staged.renameTo(dest) && dest.isFile && dest.length() == size) return true
        return try {
            staged.copyTo(dest, overwrite = true)
            val ok = dest.isFile && dest.length() == size
            if (ok) staged.delete() else dest.delete()
            ok
        } catch (_: Exception) {
            if (dest.exists() && dest.length() != size) dest.delete()
            false
        }
    }

    fun merge(media: List<Song>, privateSongs: List<Song>): List<Song> {
        if (privateSongs.isEmpty()) return media
        val known = media.mapNotNull { it.filePath }.toSet()
        return media + privateSongs.filter { song -> song.filePath !in known }
    }

    fun pageRequest(privateCount: Int, offset: Int, limit: Int): LibraryPageSlice {
        if (limit <= 0 || offset < 0) return LibraryPageSlice(0, 0, 0, 0)
        if (offset < privateCount) {
            val privateTake = minOf(limit, privateCount - offset)
            return LibraryPageSlice(
                privateOffset = offset,
                privateLimit = privateTake,
                mediaOffset = 0,
                mediaLimit = limit - privateTake,
            )
        }
        return LibraryPageSlice(
            privateOffset = 0,
            privateLimit = 0,
            mediaOffset = offset - privateCount,
            mediaLimit = limit,
        )
    }
}

data class LibraryPageSlice(
    val privateOffset: Int,
    val privateLimit: Int,
    val mediaOffset: Int,
    val mediaLimit: Int,
)
