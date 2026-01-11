package com.aethelsoft.grooveplayer.presentation.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteTracksUseCase
import com.aethelsoft.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteTracksViewModel @Inject constructor(
    application: Application,
    private val getFavoriteTracksUseCase: GetFavoriteTracksUseCase
) : AndroidViewModel(application) {
    
    private val _favoriteTracks = MutableStateFlow<List<Song>>(emptyList())
    val favoriteTracks: StateFlow<List<Song>> = _favoriteTracks.asStateFlow()
    
    init {
        loadFavoriteTracks()
    }
    
    fun loadFavoriteTracks() {
        viewModelScope.launch {
            try {
                val timestamp = TimeframeUtils.getAllTimeTimestamp()
                _favoriteTracks.value = getFavoriteTracksUseCase(timestamp, 50)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

