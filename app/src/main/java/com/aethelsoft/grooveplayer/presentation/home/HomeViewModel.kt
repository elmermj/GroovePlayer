package com.aethelsoft.grooveplayer.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteAlbumsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteArtistsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteTracksUseCase
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetLastPlayedSongsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetRecentlyPlayedUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.GetSongsUseCase
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val getSongsUseCase: GetSongsUseCase,
    private val getRecentlyPlayedUseCase: GetRecentlyPlayedUseCase,
    private val getFavoriteTracksUseCase: GetFavoriteTracksUseCase,
    private val getFavoriteArtistsUseCase: GetFavoriteArtistsUseCase,
    private val getFavoriteAlbumsUseCase: GetFavoriteAlbumsUseCase,
    private val getLastPlayedSongsUseCase: GetLastPlayedSongsUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Song>>> = _uiState.asStateFlow()
    
    // All playback history features are now reactive with Flows
    private val allTimeTimestamp = TimeframeUtils.getAllTimeTimestamp()
    
    val recentlyPlayed: StateFlow<List<Song>> = getRecentlyPlayedUseCase(20)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    
    val favoriteTracks: StateFlow<List<Song>> = getFavoriteTracksUseCase(allTimeTimestamp, 20)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    
    val favoriteArtists: StateFlow<List<FavoriteArtist>> = getFavoriteArtistsUseCase(allTimeTimestamp, 20)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    
    val favoriteAlbums: StateFlow<List<FavoriteAlbum>> = getFavoriteAlbumsUseCase(allTimeTimestamp, 20)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPlayedSongs: StateFlow<List<Song>> = getLastPlayedSongsUseCase(allTimeTimestamp, 8)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSongs()
        
        // Debug logging
        viewModelScope.launch {
            recentlyPlayed.collect { songs ->
                android.util.Log.d("HomeViewModel", "Recently played updated: ${songs.size} songs")
            }
        }
        viewModelScope.launch {
            favoriteTracks.collect { songs ->
                android.util.Log.d("HomeViewModel", "Favorite tracks updated: ${songs.size} songs")
            }
        }
        viewModelScope.launch {
            lastPlayedSongs.collect { songs ->
                android.util.Log.d("HomeViewModel", "Last played songs updated: ${songs.size} songs - ${songs.joinToString { it.title }}")
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val songs = getSongsUseCase()
                _uiState.value = UiState.Success(songs)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = e.message ?: "Failed to load songs"
                )
            }
        }
    }

    fun refresh() {
        loadSongs()
        // No need to reload favorites - they're reactive Flows that auto-update!
    }
}
