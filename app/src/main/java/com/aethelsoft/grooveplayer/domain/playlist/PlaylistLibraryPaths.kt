package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.M3uTrack
import com.aethelsoft.grooveplayer.domain.model.Song

data class PlaylistPathMatch(
    val matched: List<Song>,
    val missingLocations: List<String>,
)

/**
 * Maps M3U locations onto library songs by local file path, and writes those
 * paths back out so an export can be imported again.
 */
object PlaylistLibraryPaths {
    fun exportLocation(song: Song): String {
        val path = song.filePath?.let(::normalize).orEmpty()
        if (path.isNotEmpty()) return path
        val uri = song.uri.trim()
        if (uri.isEmpty()) return ""
        if (uri.startsWith("file:", ignoreCase = true) ||
            uri.startsWith("/") ||
            uri.matches(Regex("^[A-Za-z]:[\\\\/].*"))
        ) {
            return normalize(uri)
        }
        return uri
    }

    fun extinfTitle(song: Song): String {
        val artist = song.artist.trim()
        val title = song.title.trim()
        return when {
            artist.isEmpty() -> title
            title.isEmpty() -> artist
            else -> "$artist - $title"
        }
    }

    fun durationSeconds(song: Song): Long =
        if (song.durationMs > 0) song.durationMs / 1000 else -1L

    fun toM3uTracks(songs: List<Song>): List<M3uTrack> =
        songs.mapNotNull { song ->
            val location = exportLocation(song)
            if (location.isEmpty()) null
            else M3uTrack(
                location = location,
                title = extinfTitle(song),
                durationSeconds = durationSeconds(song),
            )
        }

    fun match(tracks: List<M3uTrack>, songs: List<Song>): PlaylistPathMatch {
        val byExact = LinkedHashMap<String, Song>()
        val byFold = LinkedHashMap<String, Song>()
        for (song in songs) {
            for (key in keysFor(song)) {
                byExact.putIfAbsent(key, song)
                byFold.putIfAbsent(key.lowercase(), song)
            }
        }
        val matched = mutableListOf<Song>()
        val missing = mutableListOf<String>()
        for (track in tracks) {
            val key = normalize(track.location)
            val song = if (key.isEmpty()) {
                null
            } else {
                byExact[key] ?: byFold[key.lowercase()]
            }
            if (song == null) missing += track.location else matched += song
        }
        return PlaylistPathMatch(matched, missing)
    }

    fun normalize(raw: String): String {
        var value = raw.trim().removePrefix("\uFEFF")
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length - 1).trim()
        }
        if (value.isEmpty()) return ""
        if (value.startsWith("content:", ignoreCase = true)) return value
        if (value.startsWith("file:", ignoreCase = true)) {
            value = pathFromFileUri(value)
        }
        value = value.replace('\\', '/')
        value = collapseSlashes(value)
        if (value.length > 1) value = value.trimEnd('/')
        return value
    }

    private fun keysFor(song: Song): List<String> {
        val keys = LinkedHashSet<String>()
        song.filePath?.let { path ->
            val normalized = normalize(path)
            if (normalized.isNotEmpty()) keys += normalized
        }
        val uri = song.uri.trim()
        if (uri.isNotEmpty()) {
            val normalized = normalize(uri)
            if (normalized.isNotEmpty()) keys += normalized
        }
        return keys.toList()
    }

    private fun pathFromFileUri(uri: String): String {
        val decoded = percentDecode(uri.substring(5)).replace('\\', '/')
        return when {
            decoded.startsWith("///") -> "/" + decoded.trimStart('/')
            decoded.startsWith("//") -> {
                val hostAndPath = decoded.removePrefix("//")
                val slash = hostAndPath.indexOf('/')
                if (slash < 0) "/" else hostAndPath.substring(slash)
            }
            else -> decoded
        }
    }

    private fun collapseSlashes(value: String): String {
        return if (value.startsWith("/")) {
            "/" + value.trimStart('/').replace(Regex("/+"), "/")
        } else {
            value.replace(Regex("/+"), "/")
        }
    }

    private fun percentDecode(value: String): String {
        val out = StringBuilder()
        var index = 0
        while (index < value.length) {
            if (value[index] == '%' && index + 2 < value.length) {
                val bytes = ArrayList<Byte>()
                var cursor = index
                while (cursor + 2 < value.length && value[cursor] == '%') {
                    val hex = value.substring(cursor + 1, cursor + 3)
                    val decoded = hex.toIntOrNull(16) ?: break
                    bytes += decoded.toByte()
                    cursor += 3
                }
                if (bytes.isNotEmpty()) {
                    out.append(bytes.toByteArray().toString(Charsets.UTF_8))
                    index = cursor
                    continue
                }
            }
            out.append(value[index])
            index++
        }
        return out.toString()
    }
}
