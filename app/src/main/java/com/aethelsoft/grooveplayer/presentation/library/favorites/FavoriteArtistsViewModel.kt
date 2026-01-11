package com.aethelsoft.grooveplayer.presentation.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.repository.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteArtistsUseCase
import com.aethelsoft.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteArtistsViewModel @Inject constructor(
    application: Application,
    private val getFavoriteArtistsUseCase: GetFavoriteArtistsUseCase
) : AndroidViewModel(application) {
    
    private val _favoriteArtists = MutableStateFlow<List<FavoriteArtist>>(emptyList())
    val favoriteArtists: StateFlow<List<FavoriteArtist>> = _favoriteArtists.asStateFlow()
    
    init {
        loadFavoriteArtists()
    }
    
    fun loadFavoriteArtists() {
        viewModelScope.launch {
            try {
                val timestamp = TimeframeUtils.getAllTimeTimestamp()
                _favoriteArtists.value = getFavoriteArtistsUseCase(timestamp, 50)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

