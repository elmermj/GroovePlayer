package com.aethelsoft.grooveplayer.presentation.library.songs.metadata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.repository.EditedSongContentHash
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.SongMetadata
import com.aethelsoft.grooveplayer.domain.usecase.SaveSongMetadataUseCase
import com.aethelsoft.grooveplayer.domain.usecase.metadata_category.ReadAudioTagsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.metadata_category.WriteAudioTagsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.player_category.GetSongMetadataUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchAlbumsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchArtistsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchGenresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EditMetadataUiState(
    val title: String = "",
    val genres: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val useAlbumYear: Boolean = false,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
    val genreSuggestions: List<String> = emptyList(),
    val artistSuggestions: List<String> = emptyList(),
    val albumSuggestions: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    /** True until file tags and library metadata have been read for this song. */
    val isLoading: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditMetadataUiState) return false
        if (title != other.title) return false
        if (genres != other.genres) return false
        if (artists != other.artists) return false
        if (album != other.album) return false
        if (year != other.year) return false
        if (trackNumber != other.trackNumber) return false
        if (useAlbumYear != other.useAlbumYear) return false
        if (artworkMimeType != other.artworkMimeType) return false
        if (genreSuggestions != other.genreSuggestions) return false
        if (artistSuggestions != other.artistSuggestions) return false
        if (albumSuggestions != other.albumSuggestions) return false
        if (isSaving != other.isSaving) return false
        if (saveError != other.saveError) return false
        if (isLoading != other.isLoading) return false
        if (artworkBytes != null) {
            if (other.artworkBytes == null) return false
            if (!artworkBytes.contentEquals(other.artworkBytes)) return false
        } else if (other.artworkBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + genres.hashCode()
        result = 31 * result + artists.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (year ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + useAlbumYear.hashCode()
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + genreSuggestions.hashCode()
        result = 31 * result + artistSuggestions.hashCode()
        result = 31 * result + albumSuggestions.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + (saveError?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

@HiltViewModel
class EditSongMetadataViewModel @Inject constructor(
    application: Application,
    private val getSongMetadataUseCase: GetSongMetadataUseCase,
    private val saveSongMetadataUseCase: SaveSongMetadataUseCase,
    private val readAudioTagsUseCase: ReadAudioTagsUseCase,
    private val writeAudioTagsUseCase: WriteAudioTagsUseCase,
    private val searchGenresUseCase: SearchGenresUseCase,
    private val searchArtistsUseCase: SearchArtistsUseCase,
    private val searchAlbumsUseCase: SearchAlbumsUseCase,
    private val editedSongContentHash: EditedSongContentHash,
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(EditMetadataUiState())
    val uiState: StateFlow<EditMetadataUiState> = _uiState.asStateFlow()
    
    private var currentSong: Song? = null
    private var loadToken = 0
    private var artworkEdited = false
    
    fun loadMetadata(song: Song) {
        currentSong = song
        artworkEdited = false
        val token = ++loadToken
        _uiState.value = EditMetadataUiState(
            title = song.title,
            genres = song.genres.map { it.name }.ifEmpty {
                if (song.genre.isNotBlank()) listOf(song.genre) else emptyList()
            },
            artists = song.artists.map { it.name }.ifEmpty {
                if (song.artist.isNotBlank()) listOf(song.artist) else emptyList()
            },
            album = song.album?.name,
            year = song.year,
            useAlbumYear = song.useAlbumYear,
            isLoading = true,
            saveError = null,
        )
        viewModelScope.launch {
            try {
                // Load from file tags first (real metadata), fallback to DB overrides
                val fileTags = readAudioTagsUseCase(song.uri)
                val dbMetadata = getSongMetadataUseCase(song.id)
                if (token != loadToken) return@launch
                _uiState.value = EditMetadataUiState(
                    title = fileTags?.title ?: dbMetadata?.title ?: song.title,
                    genres = fileTags?.genres?.ifEmpty { null } ?: dbMetadata?.genres
                        ?: (if (song.genre.isNotBlank()) listOf(song.genre) else emptyList()),
                    artists = fileTags?.artists?.ifEmpty { null } ?: dbMetadata?.artists
                        ?: (if (song.artist.isNotBlank()) listOf(song.artist) else emptyList()),
                    album = fileTags?.album ?: dbMetadata?.album ?: song.album?.name,
                    year = fileTags?.year ?: dbMetadata?.year ?: song.year,
                    trackNumber = fileTags?.trackNumber,
                    useAlbumYear = dbMetadata?.useAlbumYear ?: false,
                    artworkBytes = fileTags?.artworkBytes,
                    artworkMimeType = fileTags?.artworkMimeType,
                    isLoading = false,
                )
            } catch (error: Exception) {
                if (token != loadToken) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveError = error.message ?: "Couldn't read this song's tags",
                )
            }
        }
    }
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun updateGenres(genres: List<String>) {
        _uiState.value = _uiState.value.copy(genres = genres)
    }
    
    fun updateArtists(artists: List<String>) {
        _uiState.value = _uiState.value.copy(artists = artists)
    }
    
    fun updateAlbum(album: String?) {
        _uiState.value = _uiState.value.copy(album = album)
    }
    
    fun updateYear(year: Int?) {
        _uiState.value = _uiState.value.copy(year = year)
    }
    
    fun updateUseAlbumYear(useAlbumYear: Boolean) {
        _uiState.value = _uiState.value.copy(useAlbumYear = useAlbumYear)
    }

    fun updateTrackNumber(trackNumber: Int?) {
        _uiState.value = _uiState.value.copy(trackNumber = trackNumber)
    }

    fun updateArtwork(bytes: ByteArray?, mimeType: String?) {
        artworkEdited = true
        _uiState.value = _uiState.value.copy(artworkBytes = bytes, artworkMimeType = mimeType)
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }
    
    fun searchGenres(query: String) {
        viewModelScope.launch {
            val suggestions = searchGenresUseCase(query)
            _uiState.value = _uiState.value.copy(genreSuggestions = suggestions)
        }
    }
    
    fun searchArtists(query: String) {
        viewModelScope.launch {
            val suggestions = searchArtistsUseCase(query)
            _uiState.value = _uiState.value.copy(artistSuggestions = suggestions)
        }
    }
    
    fun searchAlbums(query: String) {
        viewModelScope.launch {
            val suggestions = searchAlbumsUseCase(query)
            _uiState.value = _uiState.value.copy(albumSuggestions = suggestions)
        }
    }
    
    suspend fun saveMetadata(): Boolean {
        val song = currentSong ?: return false
        val state = _uiState.value

        _uiState.value = state.copy(isSaving = true, saveError = null)

        val audioTags = com.aethelsoft.grooveplayer.domain.model.AudioTags(
            title = state.title,
            artists = state.artists,
            album = state.album,
            genres = state.genres,
            year = if (state.useAlbumYear) null else state.year,
            trackNumber = state.trackNumber,
            artworkBytes = state.artworkBytes,
            artworkMimeType = state.artworkMimeType
        )

        val file = editableFile(song)
        val targetUri = file?.toURI()?.toString() ?: song.uri
        val writeResult = writeAudioTagsUseCase(targetUri, audioTags, artworkEdited)
        if (writeResult.isFailure) {
            _uiState.value = state.copy(
                isSaving = false,
                saveError = writeResult.exceptionOrNull()?.message ?: "Failed to write file tags"
            )
            return false
        }
        if (file != null) {
            runCatching { editedSongContentHash.refresh(song.id, file) }
                .onFailure { error ->
                    android.util.Log.e("EditSongMetadata", "Couldn't refresh content hash", error)
                }
        }

        val metadata = SongMetadata(
            songId = song.id,
            title = state.title,
            genres = state.genres,
            artists = state.artists,
            album = state.album,
            year = if (state.useAlbumYear) null else state.year,
            useAlbumYear = state.useAlbumYear
        )
        saveSongMetadataUseCase(metadata)

        _uiState.value = state.copy(isSaving = false)
        return true
    }

    private fun editableFile(song: Song): File? {
        val fromPath = song.filePath?.takeIf { it.isNotBlank() }?.let { File(it) }
        if (fromPath != null && fromPath.isFile) return fromPath
        val uri = android.net.Uri.parse(song.uri)
        val scheme = uri.scheme
        if (scheme != null && !scheme.equals("file", ignoreCase = true)) return null
        val path = uri.path ?: return null
        return File(path).takeIf { it.isFile }
    }
}

