package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.SearchHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.entity.SearchHistoryEntity
import com.aethelsoft.grooveplayer.domain.model.SearchSuggestion
import com.aethelsoft.grooveplayer.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {
    
    override fun getRecentSuggestions(limit: Int): Flow<List<SearchSuggestion>> {
        return searchHistoryDao.getRecentSearches(limit).map { entities ->
            android.util.Log.d("SearchRepository", "Got ${entities.size} entities from database")
            val suggestions = entities.map { it.toDomain() }
            // Deduplicate suggestions
            val deduplicated = deduplicateSuggestions(suggestions)
            android.util.Log.d("SearchRepository", "Mapped to ${suggestions.size} suggestions, deduplicated to ${deduplicated.size}")
            deduplicated
        }
    }
    
    /**
     * Removes duplicate suggestions based on their unique identifiers:
     * - QuerySuggestion: by displayTitle (query string)
     * - SongSuggestion: by songId
     * - ArtistSuggestion: by artistName
     * - AlbumSuggestion: by albumName + artistName
     */
    private fun deduplicateSuggestions(suggestions: List<SearchSuggestion>): List<SearchSuggestion> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<SearchSuggestion>()
        
        for (suggestion in suggestions) {
            val key = when (suggestion) {
                is SearchSuggestion.QuerySuggestion -> "QUERY:${suggestion.displayTitle.lowercase()}"
                is SearchSuggestion.SongSuggestion -> "SONG:${suggestion.songId}"
                is SearchSuggestion.ArtistSuggestion -> "ARTIST:${suggestion.artistName.lowercase()}"
                is SearchSuggestion.AlbumSuggestion -> "ALBUM:${suggestion.albumName.lowercase()}|${suggestion.artistName.lowercase()}"
            }
            
            if (key !in seen) {
                seen.add(key)
                result.add(suggestion)
            }
        }
        
        return result
    }
    
    override suspend fun saveQuery(query: String) {
        if (query.isBlank()) return
        try {
            searchHistoryDao.insertSearchHistory(
                SearchHistoryEntity(
                    type = "QUERY",
                    query = query,
                    itemTitle = query
                )
            )
            android.util.Log.d("SearchRepository", "Saved query: $query")
        } catch (e: Exception) {
            android.util.Log.e("SearchRepository", "Error saving query: ${e.message}", e)
        }
    }
    
    override suspend fun saveSongClick(songId: String, title: String, artist: String, artworkUrl: String?) {
        searchHistoryDao.insertSearchHistory(
            SearchHistoryEntity(
                type = "SONG",
                itemId = songId,
                itemTitle = title,
                itemSubtitle = artist,
                artworkUrl = artworkUrl
            )
        )
    }
    
    override suspend fun saveArtistClick(artistName: String, artworkUrl: String?) {
        searchHistoryDao.insertSearchHistory(
            SearchHistoryEntity(
                type = "ARTIST",
                itemId = artistName,
                itemTitle = artistName,
                artworkUrl = artworkUrl
            )
        )
    }
    
    override suspend fun saveAlbumClick(albumName: String, artistName: String, artworkUrl: String?) {
        searchHistoryDao.insertSearchHistory(
            SearchHistoryEntity(
                type = "ALBUM",
                itemId = "$albumName|$artistName", // Composite key
                itemTitle = albumName,
                itemSubtitle = artistName,
                artworkUrl = artworkUrl
            )
        )
    }
    
    override suspend fun deleteSuggestion(id: String) {
        try {
            val longId = id.toLong()
            searchHistoryDao.deleteSearchHistory(longId)
        } catch (e: Exception) {
            // Ignore if ID is not a number
        }
    }
    
    override suspend fun clearAll() {
        searchHistoryDao.clearAll()
    }
    
    private fun SearchHistoryEntity.toDomain(): SearchSuggestion {
        return when (type) {
            "QUERY" -> SearchSuggestion.QuerySuggestion(
                id = id.toString(),
                displayTitle = itemTitle,
                artworkUrl = null
            )
            "SONG" -> SearchSuggestion.SongSuggestion(
                id = id.toString(),
                songId = itemId ?: "",
                displayTitle = itemTitle,
                displaySubtitle = itemSubtitle,
                artworkUrl = artworkUrl
            )
            "ARTIST" -> SearchSuggestion.ArtistSuggestion(
                id = id.toString(),
                artistName = itemId ?: itemTitle,
                displayTitle = itemTitle,
                artworkUrl = artworkUrl
            )
            "ALBUM" -> {
                val parts = itemId?.split("|") ?: listOf(itemTitle, itemSubtitle ?: "")
                SearchSuggestion.AlbumSuggestion(
                    id = id.toString(),
                    albumName = parts.getOrElse(0) { itemTitle },
                    artistName = parts.getOrElse(1) { itemSubtitle ?: "" },
                    displayTitle = itemTitle,
                    displaySubtitle = itemSubtitle,
                    artworkUrl = artworkUrl
                )
            }
            else -> SearchSuggestion.QuerySuggestion(
                id = id.toString(),
                displayTitle = itemTitle
            )
        }
    }
}
