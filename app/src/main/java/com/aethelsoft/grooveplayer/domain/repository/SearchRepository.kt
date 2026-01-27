package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.SearchSuggestion
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun getRecentSuggestions(limit: Int = 10): Flow<List<SearchSuggestion>>
    suspend fun saveQuery(query: String)
    suspend fun saveSongClick(songId: String, title: String, artist: String, artworkUrl: String?)
    suspend fun saveArtistClick(artistName: String, artworkUrl: String?)
    suspend fun saveAlbumClick(albumName: String, artistName: String, artworkUrl: String?)
    suspend fun deleteSuggestion(id: String)
    suspend fun clearAll()
}
