package com.aethelsoft.grooveplayer.presentation.library.songs

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aethelsoft.grooveplayer.data.paging.SongsPagingSource
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    val songsPagingFlow: Flow<PagingData<Song>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { SongsPagingSource(musicRepository) }
    ).flow

    private val _selectedSongForEdit = MutableStateFlow<Song?>(null)
    val selectedSongForEdit: StateFlow<Song?> = _selectedSongForEdit.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun setSelectedSongForEdit(song: Song?) {
        _selectedSongForEdit.value = song
    }

    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    fun enterSelectionModeWithSong(song: Song) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(song.id)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { ids ->
            if (ids.contains(id)) ids - id else ids + id
        }
    }

    fun addToSelection(id: String) {
        _selectedIds.update { it + id }
    }

    fun removeFromSelection(id: String) {
        _selectedIds.update { it - id }
    }

    fun clearSelectionAndExit() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun setSelectedIds(ids: Set<String>) {
        _selectedIds.value = ids
    }

    fun getSelectedSongs(allSongs: List<Song>): List<Song> {
        val ids = _selectedIds.value
        return allSongs.filter { it.id in ids }
    }
}


