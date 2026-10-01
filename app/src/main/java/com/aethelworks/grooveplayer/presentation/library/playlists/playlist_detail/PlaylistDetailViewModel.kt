package com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.playlist.InvalidPlaylistNameException
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.AddSongsToPlaylistUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.DeletePlaylistUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.ExportM3uPlaylistUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.GetSongRecommendationsUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.ObservePlaylistUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.RemovePlaylistTrackUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.RenamePlaylistUseCase
import com.aethelworks.grooveplayer.domain.usecase.playlist_category.ReorderPlaylistUseCase
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
    private val getSongRecommendationsUseCase: GetSongRecommendationsUseCase,
    private val addSongsToPlaylistUseCase: AddSongsToPlaylistUseCase,
) : ViewModel() {

    val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: 0L

    private val _playlist = MutableStateFlow<PlaylistWithTracks?>(null)
    val playlist: StateFlow<PlaylistWithTracks?> = _playlist.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRecommendationShown = MutableStateFlow(false)
    val isRecommendationShown: StateFlow<Boolean> = _isRecommendationShown.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _recommends = MutableStateFlow<List<Song>>(emptyList())
    val recommends: StateFlow<List<Song>> = _recommends.asStateFlow()


    private val edits = Mutex()
    private var recommendationLoad = 0

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

    fun showRecommendations() {
        val load = ++recommendationLoad
        viewModelScope.launch {
            try {
                val songs = getSongRecommendationsUseCase()
                if (load != recommendationLoad) return@launch
                _recommends.value = songs
                _isRecommendationShown.value = true
            } catch (e: Exception) {
                if (load != recommendationLoad) return@launch
                _message.value = e.message ?: "Could not load recommendations"
            }
        }
    }

    fun dismissRecommendations() {
        _recommends.value = emptyList()
    }

    fun dismissSheet() {
        recommendationLoad++
        _isRecommendationShown.value = false
    }

    fun onSwipeToDismissRecommendationItem(song: Song) {
        removeRecommendation(song)
    }

    fun addToPlaylist(song: Song) {
        viewModelScope.launch {
            edits.withLock {
                try {
                    addSongsToPlaylistUseCase(playlistId, listOf(song))
                    removeRecommendation(song)
                } catch (e: Exception) {
                    _message.value = e.message ?: "Could not add song"
                }
            }
        }
    }

    fun addAll(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            edits.withLock {
                try {
                    addSongsToPlaylistUseCase(playlistId, songs)
                    dismissRecommendations()
                    dismissSheet()
                } catch (e: Exception) {
                    _message.value = e.message ?: "Could not add songs"
                }
            }
        }
    }

    private fun removeRecommendation(song: Song) {
        _recommends.value = _recommends.value.filterNot { it.id == song.id }
        if (_recommends.value.isEmpty()) dismissSheet()
    }
}
