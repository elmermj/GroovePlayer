package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.Song
import java.io.File

/** A private-library song that can be named from an M3U line. */
data class PlaylistLibrarySong(
    val song: Song,
    val contentHash: String,
    val fileName: String,
)

data class PlaylistHashMatch(
    val song: Song,
    val contentHash: String,
)

/**
 * Match an M3U location to a library song by path or file name, then by
 * SHA-256 when the bytes were readable. A miss is not filled with another song.
 */
object PlaylistM3uMatch {
    fun match(
        location: String,
        readableHash: String?,
        library: List<PlaylistLibrarySong>,
    ): PlaylistHashMatch? {
        val byPath = uniquePathOrName(location, library)
        val hash = readableHash?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (byPath != null && (hash == null || byPath.contentHash.equals(hash, ignoreCase = true))) {
            return PlaylistHashMatch(byPath.song, byPath.contentHash.lowercase())
        }
        if (hash == null) return null
        val byHash = library.filter { it.contentHash.equals(hash, ignoreCase = true) }
        val chosen = byHash.singleOrNull()
            ?: byHash.minByOrNull { it.song.id }
        return chosen?.let { PlaylistHashMatch(it.song, it.contentHash.lowercase()) }
    }

    fun exportFileName(song: Song): String = fileName(song.filePath.orEmpty())

    fun fileName(location: String): String {
        val path = normalize(location)
        if (path.isEmpty() || path.startsWith("content:", ignoreCase = true)) return ""
        return path.substringAfterLast('/').trim()
    }

    /** Local file behind an M3U path, when the process can read it. Never a content URI. */
    fun readableFile(location: String): File? {
        val path = normalize(location)
        if (path.isEmpty() || path.startsWith("content:", ignoreCase = true)) return null
        if (path.contains("://")) return null
        val file = File(path)
        return file.takeIf { it.isFile && it.canRead() }
    }

    private fun uniquePathOrName(location: String, library: List<PlaylistLibrarySong>): PlaylistLibrarySong? {
        val path = normalize(location)
        val name = fileName(location)
        if (path.isNotEmpty()) {
            val byPath = library.filter { normalize(it.song.filePath.orEmpty()) == path }
            if (byPath.size == 1) return byPath.first()
        }
        if (name.isEmpty()) return null
        val byName = library.filter { it.fileName.equals(name, ignoreCase = true) }
        return byName.singleOrNull()
    }

    private fun normalize(raw: String): String {
        var value = raw.trim().removePrefix("\uFEFF")
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length - 1).trim()
        }
        if (value.isEmpty()) return ""
        if (value.startsWith("content:", ignoreCase = true)) return value
        if (value.startsWith("file:", ignoreCase = true)) {
            value = value.substring(5).let { rest ->
                when {
                    rest.startsWith("///") -> "/" + rest.trimStart('/')
                    rest.startsWith("//") -> {
                        val hostAndPath = rest.removePrefix("//")
                        val slash = hostAndPath.indexOf('/')
                        if (slash < 0) "" else hostAndPath.substring(slash)
                    }
                    else -> rest
                }
            }
        }
        return value.replace('\\', '/')
    }
}
