package com.aethelsoft.grooveplayer.presentation.library.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aethelsoft.grooveplayer.data.paging.ArtistSongsPagingSource
import com.aethelsoft.grooveplayer.data.paging.EmptySongsPagingSource
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val artistIdFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val songsPagingFlow: Flow<PagingData<Song>> = combine(
        artistIdFlow,
        musicRepository.catalogGeneration
    ) { artistId, _ -> artistId }
        .flatMapLatest { artistId ->
            if (artistId != null) {
                Pager(
                    config = PagingConfig(pageSize = 50, enablePlaceholders = false),
                    pagingSourceFactory = { ArtistSongsPagingSource(musicRepository, artistId) }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(pageSize = 50),
                    pagingSourceFactory = { EmptySongsPagingSource() }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    fun load(artistId: String) {
        artistIdFlow.value = artistId
    }
}
