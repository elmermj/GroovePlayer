package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.M3uDocument
import com.aethelsoft.grooveplayer.domain.model.M3uTrack

/**
 * Extended M3U reader and writer. Locations are preserved as written so a
 * serialize/parse round trip keeps local library paths.
 */
object M3uCodec {
    fun parse(text: String): M3uDocument {
        val lines = text.removePrefix("\uFEFF").split(Regex("\\r\\n|\\n|\\r"))
        var name: String? = null
        var pendingTitle: String? = null
        var pendingDuration = -1L
        var hasPendingInfo = false
        val tracks = mutableListOf<M3uTrack>()

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#")) {
                when {
                    line.startsWith("#EXTINF:", ignoreCase = true) -> {
                        val payload = line.substringAfter(':')
                        val comma = payload.indexOf(',')
                        val durationPart = if (comma >= 0) payload.substring(0, comma) else payload
                        val titlePart = if (comma >= 0) payload.substring(comma + 1) else ""
                        val durationToken = durationPart.trim()
                            .substringBefore(' ')
                            .substringBefore('.')
                        pendingDuration = durationToken.toLongOrNull() ?: -1L
                        pendingTitle = titlePart.trim().ifEmpty { null }
                        hasPendingInfo = true
                    }
                    line.startsWith("#PLAYLIST:", ignoreCase = true) -> {
                        val parsedName = line.substringAfter(':').trim()
                        if (parsedName.isNotEmpty()) name = parsedName
                    }
                }
                continue
            }
            val location = line.removeSurrounding("\"").trim()
            if (location.isEmpty()) continue
            tracks += M3uTrack(
                location = location,
                title = if (hasPendingInfo) pendingTitle else null,
                durationSeconds = if (hasPendingInfo) pendingDuration else -1L,
            )
            hasPendingInfo = false
            pendingTitle = null
            pendingDuration = -1L
        }
        return M3uDocument(name = name, tracks = tracks)
    }

    fun serialize(name: String?, tracks: List<M3uTrack>): String = buildString {
        append("#EXTM3U\n")
        val playlistName = name
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
        if (!playlistName.isNullOrEmpty()) {
            append("#PLAYLIST:")
            append(playlistName)
            append('\n')
        }
        for (track in tracks) {
            val location = track.location.trim()
            if (location.isEmpty() || location.contains('\n') || location.contains('\r')) continue
            val title = track.title
                ?.replace('\n', ' ')
                ?.replace('\r', ' ')
                ?.trim()
                .orEmpty()
            append("#EXTINF:")
            append(track.durationSeconds)
            append(',')
            append(title)
            append('\n')
            append(location)
            append('\n')
        }
    }

    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return bytes.decodeToString(3)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return bytes.decodeToString()
    }
}
