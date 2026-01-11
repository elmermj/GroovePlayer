package com.aethelsoft.grooveplayer.presentation.library.songs.metadata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.SongMetadata
import com.aethelsoft.grooveplayer.domain.usecase.player_category.GetSongMetadataUseCase
import com.aethelsoft.grooveplayer.domain.usecase.SaveSongMetadataUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchAlbumsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchArtistsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.search_category.SearchGenresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditMetadataUiState(
    val title: String = "",
    val genres: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val year: Int? = null,
    val useAlbumYear: Boolean = false,
    val genreSuggestions: List<String> = emptyList(),
    val artistSuggestions: List<String> = emptyList(),
    val albumSuggestions: List<String> = emptyList()
)

@HiltViewModel
class EditSongMetadataViewModel @Inject constructor(
    application: Application,
    private val getSongMetadataUseCase: GetSongMetadataUseCase,
    private val saveSongMetadataUseCase: SaveSongMetadataUseCase,
    private val searchGenresUseCase: SearchGenresUseCase,
    private val searchArtistsUseCase: SearchArtistsUseCase,
    private val searchAlbumsUseCase: SearchAlbumsUseCase
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(EditMetadataUiState())
    val uiState: StateFlow<EditMetadataUiState> = _uiState.asStateFlow()
    
    private var currentSong: Song? = null
    
    fun loadMetadata(song: Song) {
        currentSong = song
        viewModelScope.launch {
            val metadata = getSongMetadataUseCase(song.id)
            _uiState.value = EditMetadataUiState(
                title = metadata?.title ?: song.title,
                genres = metadata?.genres ?: (if (song.genre.isNotBlank()) listOf(song.genre) else emptyList()),
                artists = metadata?.artists ?: (if (song.artist.isNotBlank()) listOf(song.artist) else emptyList()),
                album = metadata?.album ?: song.album,
                year = metadata?.year,
                useAlbumYear = metadata?.useAlbumYear ?: false
            )
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
    
    suspend fun saveMetadata() {
        val song = currentSong ?: return
        val state = _uiState.value
        
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
    }
}

