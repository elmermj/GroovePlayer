package com.aethelworks.grooveplayer.presentation.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.domain.repository.SearchRepository
import com.aethelworks.grooveplayer.domain.repository.SongMetadataRepository
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchBarViewModel @Inject constructor(
    application: Application,
    val searchRepository: SearchRepository,
    private val songMetadataRepository: SongMetadataRepository,
    private val musicRepository: com.aethelworks.grooveplayer.domain.repository.MusicRepository
) : AndroidViewModel(application) {
    
    val suggestions: StateFlow<List<com.aethelworks.grooveplayer.domain.model.SearchSuggestion>> = 
        searchRepository.getRecentSuggestions(15)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    fun saveQuery(query: String) {
        viewModelScope.launch {
            searchRepository.saveQuery(query)
        }
    }
    
    fun saveSongClick(songId: String, title: String, artist: String, artworkUrl: String?) {
        viewModelScope.launch {
            searchRepository.saveSongClick(songId, title, artist, artworkUrl)
        }
    }
    
    fun saveArtistClick(artistName: String, artworkUrl: String?) {
        viewModelScope.launch {
            searchRepository.saveArtistClick(artistName, artworkUrl)
        }
    }
    
    fun saveAlbumClick(albumName: String, artistName: String, artworkUrl: String?) {
        viewModelScope.launch {
            searchRepository.saveAlbumClick(albumName, artistName, artworkUrl)
        }
    }
    
    suspend fun getArtistArtwork(artistName: String): String? {
        return try {
            val artist = songMetadataRepository.getArtist(artistName)
            artist?.imageUrl ?: run {
                val album = songMetadataRepository.getLatestAlbumByArtist(artistName)
                album?.artworkUrl
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getAlbumArtwork(albumName: String, artistName: String): String? {
        return try {
            val album = songMetadataRepository.getAlbum(albumName, artistName)
            album?.artworkUrl
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getAllSongs(): List<com.aethelworks.grooveplayer.domain.model.Song> {
        return musicRepository.getAllSongs()
    }
}
