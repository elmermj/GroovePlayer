package com.aethelsoft.grooveplayer.presentation.library.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.player_category.GetSongsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.AddSongsToPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPlaylistTracksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSongsUseCase: GetSongsUseCase,
    private val addSongsToPlaylistUseCase: AddSongsToPlaylistUseCase,
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: 0L

    private val allSongs = MutableStateFlow<List<Song>>(emptyList())
    val query = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val songs: StateFlow<List<Song>> = combine(allSongs, query) { songs, rawQuery ->
        val needle = rawQuery.trim()
        if (needle.isEmpty()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(needle, ignoreCase = true) ||
                    song.artist.contains(needle, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            try {
                allSongs.value = getSongsUseCase()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun add(selectedIds: Set<String>, onDone: () -> Unit) {
        val chosen = allSongs.value.filter { it.id in selectedIds }
        if (chosen.isEmpty()) return
        viewModelScope.launch {
            addSongsToPlaylistUseCase(playlistId, chosen)
            onDone()
        }
    }
}
