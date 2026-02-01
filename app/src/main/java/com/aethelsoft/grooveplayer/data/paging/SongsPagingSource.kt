package com.aethelsoft.grooveplayer.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository

private const val PAGE_SIZE = 25

/**
 * PagingSource for All Songs screen. Loads songs in pages from MediaStore.
 */
class SongsPagingSource(
    private val musicRepository: MusicRepository
) : PagingSource<Int, Song>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val offset = params.key ?: 0
        return try {
            val page = musicRepository.getSongsPage(offset = offset, limit = params.loadSize)
            val nextKey = if (page.size < params.loadSize) null else offset + page.size
            LoadResult.Page(
                data = page,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { pos ->
            (pos / PAGE_SIZE) * PAGE_SIZE
        }
    }
}
