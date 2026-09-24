package com.aethelsoft.grooveplayer.presentation.library.playlists

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.M3uImportResult
import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.playlist.InvalidPlaylistNameException
import com.aethelsoft.grooveplayer.domain.playlist.M3uCodec
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.CreatePlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.DeletePlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ExportM3uPlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ImportM3uPlaylistUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.ObservePlaylistsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.playlist_category.RenamePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val importM3uPlaylistUseCase: ImportM3uPlaylistUseCase,
    private val exportM3uPlaylistUseCase: ExportM3uPlaylistUseCase,
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importResult = MutableStateFlow<M3uImportResult?>(null)
    val importResult: StateFlow<M3uImportResult?> = _importResult.asStateFlow()

    init {
        viewModelScope.launch {
            observePlaylistsUseCase().collect { playlists ->
                _playlists.value = playlists
                _isLoading.value = false
            }
        }
    }

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = createPlaylistUseCase(name)
                _message.value = null
                onCreated(id)
            } catch (e: InvalidPlaylistNameException) {
                _message.value = e.message
            }
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch {
            try {
                renamePlaylistUseCase(id, name)
                _message.value = null
            } catch (e: InvalidPlaylistNameException) {
                _message.value = e.message
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deletePlaylistUseCase(id)
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error("Could not read that file")
                val displayName = withContext(Dispatchers.IO) { displayName(uri) }
                val requestedName = PlaylistNames.fromDisplayName(displayName)
                _importResult.value = importM3uPlaylistUseCase(
                    requestedName,
                    M3uCodec.decode(bytes),
                )
            } catch (e: Exception) {
                _importResult.value = M3uImportResult.Failure(
                    e.message ?: "Could not import that M3U file",
                )
            }
        }
    }

    fun exportTo(playlist: Playlist, uri: Uri) {
        viewModelScope.launch {
            try {
                val text = exportM3uPlaylistUseCase(playlist.id)
                    ?: error("Playlist not found")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not write that file")
                }
                _message.value = "Exported ${playlist.name}"
            } catch (e: Exception) {
                _message.value = e.message ?: "Export failed"
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun dismissImportResult() {
        _importResult.value = null
    }

    private fun displayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
