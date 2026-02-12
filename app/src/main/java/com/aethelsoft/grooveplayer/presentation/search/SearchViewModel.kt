package com.aethelsoft.grooveplayer.presentation.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.SongMetadataRepository
import com.aethelsoft.grooveplayer.domain.repository.SearchRepository
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchAlbumsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchArtistsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    val searchSongsUseCase: SearchSongsUseCase,
    private val searchArtistsUseCase: SearchArtistsUseCase,
    private val searchAlbumsUseCase: SearchAlbumsUseCase,
    private val songMetadataRepository: SongMetadataRepository,
    val searchRepository: SearchRepository,
    private val musicRepository: MusicRepository
) : AndroidViewModel(application) {
    
    suspend fun getAllSongs(): List<Song> {
        return musicRepository.getAllSongs()
    }
    
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    
    private val _artists = MutableStateFlow<List<String>>(emptyList())
    val artists: StateFlow<List<String>> = _artists.asStateFlow()
    
    private val _albums = MutableStateFlow<List<String>>(emptyList())
    val albums: StateFlow<List<String>> = _albums.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }
    
    fun search(query: String) {
        if (query.isBlank()) {
            _songs.value = emptyList()
            _artists.value = emptyList()
            _albums.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = searchSongsUseCase(query)
                _artists.value = searchArtistsUseCase(query)
                _albums.value = searchAlbumsUseCase(query)
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error searching: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    suspend fun getArtistArtwork(artistName: String): String? {
        return try {
            // First try to get artist image
            val artist = songMetadataRepository.getArtist(artistName)
            artist?.imageUrl ?: run {
                // If no artist image, get latest album artwork
                val album = songMetadataRepository.getLatestAlbumByArtist(artistName)
                album?.artworkUrl
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Error getting artist artwork: ${e.message}", e)
            null
        }
    }
    
    suspend fun getAlbumArtwork(albumName: String, artistName: String): String? {
        return try {
            val album = songMetadataRepository.getAlbum(albumName, artistName)
            album?.artworkUrl
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Error getting album artwork: ${e.message}", e)
            null
        }
    }
}
