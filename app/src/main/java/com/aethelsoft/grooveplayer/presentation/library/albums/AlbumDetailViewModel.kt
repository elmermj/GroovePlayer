package com.aethelsoft.grooveplayer.presentation.library.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.parseAlbumId
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    fun load(albumId: String) {
        viewModelScope.launch {
            val (artistName, albumName) = parseAlbumId(albumId)
            val allSongs = musicRepository.getAllSongs()
            _songs.value = if (artistName.isNotEmpty()) {
                allSongs.filter { it.album?.name == albumName && it.artist == artistName }
            } else {
                allSongs.filter { it.album?.name == albumName }
            }
        }
    }
}

