package com.aethelworks.grooveplayer.presentation.library.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aethelworks.grooveplayer.data.paging.AlbumSongsPagingSource
import com.aethelworks.grooveplayer.data.paging.EmptySongsPagingSource
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.model.parseAlbumId
import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val albumIdFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val songsPagingFlow: Flow<PagingData<Song>> = combine(
        albumIdFlow,
        musicRepository.catalogGeneration
    ) { albumId, _ -> albumId }
        .flatMapLatest { albumId ->
            if (albumId != null) {
                val (_, albumName) = parseAlbumId(albumId)
                Pager(
                    config = PagingConfig(pageSize = 50, enablePlaceholders = false),
                    pagingSourceFactory = { AlbumSongsPagingSource(musicRepository, albumName) }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(pageSize = 50),
                    pagingSourceFactory = { EmptySongsPagingSource() }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    fun load(albumId: String) {
        albumIdFlow.value = albumId
    }
}
