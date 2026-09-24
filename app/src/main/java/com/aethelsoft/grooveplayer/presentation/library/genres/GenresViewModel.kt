package com.aethelsoft.grooveplayer.presentation.library.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.LibraryGenre
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.usecase.library_category.GetLibraryGenresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenresViewModel @Inject constructor(
    private val getLibraryGenresUseCase: GetLibraryGenresUseCase,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _genres = MutableStateFlow<List<LibraryGenre>>(emptyList())
    val genres: StateFlow<List<LibraryGenre>> = _genres.asStateFlow()

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
            _genres.value = getLibraryGenresUseCase()
        } catch (e: Exception) {
            android.util.Log.e("GenresViewModel", "Failed to load genres", e)
            _genres.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }
}
