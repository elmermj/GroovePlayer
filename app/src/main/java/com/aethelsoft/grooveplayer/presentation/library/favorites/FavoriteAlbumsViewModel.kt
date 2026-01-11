package com.aethelsoft.grooveplayer.presentation.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.repository.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetFavoriteAlbumsUseCase
import com.aethelsoft.grooveplayer.utils.TimeframeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteAlbumsViewModel @Inject constructor(
    application: Application,
    private val getFavoriteAlbumsUseCase: GetFavoriteAlbumsUseCase
) : AndroidViewModel(application) {
    
    private val _favoriteAlbums = MutableStateFlow<List<FavoriteAlbum>>(emptyList())
    val favoriteAlbums: StateFlow<List<FavoriteAlbum>> = _favoriteAlbums.asStateFlow()
    
    init {
        loadFavoriteAlbums()
    }
    
    fun loadFavoriteAlbums() {
        viewModelScope.launch {
            try {
                val timestamp = TimeframeUtils.getAllTimeTimestamp()
                _favoriteAlbums.value = getFavoriteAlbumsUseCase(timestamp, 50)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

