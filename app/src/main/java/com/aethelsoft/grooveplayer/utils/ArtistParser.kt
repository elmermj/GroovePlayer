package com.aethelsoft.grooveplayer.utils

object ArtistParser {

    fun parseArtists(artistString: String?): List<String> {
        if (artistString.isNullOrBlank()) {
            return listOf("Unknown Artist")
        }

        var normalized = artistString.trim()
        val featuringPatterns = listOf(
            " feat. ",
            " feat ",
            " ft. ",
            " ft ",
            " featuring ",
            "/"
        )
        
        featuringPatterns.forEach { pattern ->
            normalized = normalized.replace(pattern, ", ", ignoreCase = true)
        }

        val artists = normalized
            .split(Regex("[,;]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        
        return if (artists.isEmpty()) {
            listOf(artistString.trim())
        } else {
            artists
        }
    }
    
    /**
     * Gets the primary (first) artist from an artist string.
     */
    fun getPrimaryArtist(artistString: String?): String {
        return parseArtists(artistString).firstOrNull() ?: "Unknown Artist"
    }
}
