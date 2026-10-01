package com.aethelworks.grooveplayer.presentation.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.domain.library.SongLikeIndex
import com.aethelworks.grooveplayer.domain.usecase.home_category.GetFavoriteTracksUseCase
import com.aethelworks.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteTracksViewModel @Inject constructor(
    application: Application,
    private val getFavoriteTracksUseCase: GetFavoriteTracksUseCase
) : AndroidViewModel(application) {

    val favoriteTracks by lazy {
        getFavoriteTracksUseCase(TimeframeUtils.getAllTimeTimestamp(), SongLikeIndex.ALL)
    }
    
    init {
        loadFavoriteTracks()
    }
    
    fun loadFavoriteTracks() {
        viewModelScope.launch {
            try {
                val timestamp = TimeframeUtils.getAllTimeTimestamp()
                favoriteTracks
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

