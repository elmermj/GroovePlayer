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
}


