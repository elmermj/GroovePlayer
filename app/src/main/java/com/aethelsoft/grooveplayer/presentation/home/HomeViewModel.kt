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
    
    val recentlyPlayed: StateFlow<List<Song>> = getRecentlyPlayedUseCase(20)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    
    private val _favoriteTracks = MutableStateFlow<List<Song>>(emptyList())
    val favoriteTracks: StateFlow<List<Song>> = _favoriteTracks.asStateFlow()
    
    private val _favoriteArtists = MutableStateFlow<List<FavoriteArtist>>(emptyList())
    val favoriteArtists: StateFlow<List<FavoriteArtist>> = _favoriteArtists.asStateFlow()
    
    private val _favoriteAlbums = MutableStateFlow<List<FavoriteAlbum>>(emptyList())
    val favoriteAlbums: StateFlow<List<FavoriteAlbum>> = _favoriteAlbums.asStateFlow()

    private val _lastPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val lastPlayedSongs: StateFlow<List<Song>> = _lastPlayedSongs.asStateFlow()

    init {
        loadSongs()
        loadFavorites()
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
    
    fun loadFavorites() {
        viewModelScope.launch {
            try {
                // Load favorites for all-time (using 0 as timestamp)
                val allTimeTimestamp = TimeframeUtils.getAllTimeTimestamp()
                _favoriteTracks.value = getFavoriteTracksUseCase(allTimeTimestamp, 20)
                _favoriteArtists.value = getFavoriteArtistsUseCase(allTimeTimestamp, 20)
                _favoriteAlbums.value = getFavoriteAlbumsUseCase(allTimeTimestamp, 20)
            } catch (e: Exception) {
                // TODO: Silently fail - favorites are optional
            }
        }
    }

    fun refresh() {
        loadSongs()
        loadFavorites()
    }
}
