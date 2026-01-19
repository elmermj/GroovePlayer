package com.aethelsoft.grooveplayer.presentation.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteAlbumsUseCase
import com.aethelsoft.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteAlbumsViewModel @Inject constructor(
    application: Application,
    private val getFavoriteAlbumsUseCase: GetFavoriteAlbumsUseCase
) : AndroidViewModel(application) {
    
//    private val _favoriteAlbums = MutableStateFlow<List<FavoriteAlbum>>(emptyList())
//    val favoriteAlbums: StateFlow<List<FavoriteAlbum>> = _favoriteAlbums.asStateFlow()

    val favoriteAlbums by lazy {
        getFavoriteAlbumsUseCase(TimeframeUtils.getAllTimeTimestamp(), 50)
    }

    init {
        loadFavoriteAlbums()
    }
    
    fun loadFavoriteAlbums() {
        viewModelScope.launch {
            try {
                val timestamp = TimeframeUtils.getAllTimeTimestamp()
                favoriteAlbums
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

