package com.aethelsoft.grooveplayer.presentation.library.albums

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aethelsoft.grooveplayer.data.paging.AlbumSongsPagingSource
import com.aethelsoft.grooveplayer.data.paging.EmptySongsPagingSource
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.parseAlbumId
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val albumIdFlow = MutableStateFlow<String?>(null)

    val songsPagingFlow: Flow<PagingData<Song>> = albumIdFlow.flatMapLatest { albumId ->
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

    fun load(albumId: String) {
        albumIdFlow.value = albumId
    }
}

