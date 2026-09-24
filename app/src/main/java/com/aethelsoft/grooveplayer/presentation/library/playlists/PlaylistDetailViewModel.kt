package com.aethelsoft.grooveplayer.presentation.library.playlists

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelsoft.grooveplayer.domain.playlist.InvalidPlaylistNameException
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.DeletePlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ExportM3uPlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ObservePlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.RemovePlaylistTrackUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.RenamePlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ReorderPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    observePlaylistUseCase: ObservePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val removePlaylistTrackUseCase: RemovePlaylistTrackUseCase,
    private val reorderPlaylistUseCase: ReorderPlaylistUseCase,
    private val exportM3uPlaylistUseCase: ExportM3uPlaylistUseCase,
) : ViewModel() {

    val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: 0L

    private val _playlist = MutableStateFlow<PlaylistWithTracks?>(null)
    val playlist: StateFlow<PlaylistWithTracks?> = _playlist.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val edits = Mutex()

    init {
        viewModelScope.launch {
            observePlaylistUseCase(playlistId).collect { playlist ->
                _playlist.value = playlist
                _isLoading.value = false
            }
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            try {
                renamePlaylistUseCase(playlistId, name)
                _message.value = null
            } catch (e: InvalidPlaylistNameException) {
                _message.value = e.message
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId)
            onDeleted()
        }
    }

    fun removeTrack(entryId: Long) {
        viewModelScope.launch {
            edits.withLock {
                removePlaylistTrackUseCase(playlistId, entryId)
            }
        }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            edits.withLock {
                reorderPlaylistUseCase(playlistId, fromIndex, toIndex)
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = exportM3uPlaylistUseCase(playlistId) ?: error("Playlist not found")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not write that file")
                }
                _message.value = "Exported playlist"
            } catch (e: Exception) {
                _message.value = e.message ?: "Export failed"
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
