package com.aethelsoft.grooveplayer.domain.model

/**
 * Represents a search suggestion that can be either a query string or a clicked item
 */
sealed class SearchSuggestion {
    abstract val id: String
    abstract val displayTitle: String
    abstract val displaySubtitle: String?
    abstract val artworkUrl: String?
    
    data class QuerySuggestion(
        override val id: String,
        override val displayTitle: String,
        override val displaySubtitle: String? = null,
        override val artworkUrl: String? = null
    ) : SearchSuggestion()
    
    data class SongSuggestion(
        override val id: String,
        val songId: String,
        override val displayTitle: String,
        override val displaySubtitle: String?,
        override val artworkUrl: String?
    ) : SearchSuggestion()
    
    data class ArtistSuggestion(
        override val id: String,
        val artistName: String,
        override val displayTitle: String,
        override val displaySubtitle: String? = null,
        override val artworkUrl: String?
    ) : SearchSuggestion()
    
    data class AlbumSuggestion(
        override val id: String,
        val albumName: String,
        val artistName: String,
        override val displayTitle: String,
        override val displaySubtitle: String?,
        override val artworkUrl: String?
    ) : SearchSuggestion()
}
