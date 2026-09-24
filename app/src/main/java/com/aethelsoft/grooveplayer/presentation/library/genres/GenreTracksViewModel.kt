package com.aethelsoft.grooveplayer.presentation.library.genres

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.usecase.library_category.GetSongsByGenreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenreTracksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSongsByGenreUseCase: GetSongsByGenreUseCase,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    val genreName: String = Uri.decode(savedStateHandle.get<String>("genreName").orEmpty())

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            musicRepository.catalogGeneration.collect {
                load()
            }
        }
    }

    private suspend fun load() {
        _isLoading.value = true
        try {
            _songs.value = getSongsByGenreUseCase(genreName)
        } catch (e: Exception) {
            android.util.Log.e("GenreTracksViewModel", "Failed to load genre tracks", e)
            _songs.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }
}
