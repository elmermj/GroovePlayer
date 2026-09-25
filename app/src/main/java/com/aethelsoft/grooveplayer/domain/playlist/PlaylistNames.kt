package com.aethelsoft.grooveplayer.domain.playlist

object PlaylistNames {
    const val MAX_LENGTH = 120

    fun sanitize(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    fun validationError(name: String): String? {
        val sanitized = sanitize(name)
        return when {
            sanitized.isEmpty() -> "Playlist name cannot be empty"
            sanitized.length > MAX_LENGTH -> "Playlist name is too long"
            else -> null
        }
    }

    /** Display name from a document picker, without the extension. */
    fun fromDisplayName(displayName: String?): String {
        if (displayName.isNullOrBlank()) return ""
        val base = displayName.substringBeforeLast('.').ifBlank { displayName }
        return sanitize(base)
    }

    fun fileName(name: String): String {
        val base = sanitize(name)
            .ifBlank { "playlist" }
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
        return "$base.m3u"
    }
}

class InvalidPlaylistNameException(message: String) : IllegalArgumentException(message)
