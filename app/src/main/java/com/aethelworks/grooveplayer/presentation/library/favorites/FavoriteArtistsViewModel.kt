package com.aethelworks.grooveplayer.presentation.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.domain.library.SongLikeIndex
import com.aethelworks.grooveplayer.domain.usecase.home_category.GetFavoriteArtistsUseCase
import com.aethelworks.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteArtistsViewModel @Inject constructor(
    application: Application,
    private val getFavoriteArtistsUseCase: GetFavoriteArtistsUseCase
) : AndroidViewModel(application) {

    val favoriteArtists by lazy {
        getFavoriteArtistsUseCase(TimeframeUtils.getAllTimeTimestamp(), SongLikeIndex.ALL)
    }

    init {
        loadFavoriteArtists()
    }
    
    fun loadFavoriteArtists() {
        viewModelScope.launch {
            try {
                val timestamp = TimeframeUtils.getAllTimeTimestamp()
                favoriteArtists
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

