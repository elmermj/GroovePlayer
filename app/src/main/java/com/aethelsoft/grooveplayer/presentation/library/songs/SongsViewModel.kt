package com.aethelsoft.grooveplayer.presentation.library.songs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.player_category.GetSongsUseCase
import com.aethelsoft.grooveplayer.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsViewModel @Inject constructor(
    application: Application,
    private val getSongsUseCase: GetSongsUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Song>>> = _uiState.asStateFlow()

    init {
        loadSongs()
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
    }
}


