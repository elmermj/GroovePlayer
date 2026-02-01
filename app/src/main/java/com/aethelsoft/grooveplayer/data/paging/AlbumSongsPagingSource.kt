package com.aethelsoft.grooveplayer.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository

/**
 * PagingSource for Album detail screen. Loads songs by album in pages.
 */
class AlbumSongsPagingSource(
    private val musicRepository: MusicRepository,
    private val album: String
) : PagingSource<Int, Song>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val offset = params.key ?: 0
        return try {
            val page = musicRepository.getSongsByAlbumPage(album, offset, params.loadSize)
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
        return state.anchorPosition?.let { (it / PAGE_SIZE) * PAGE_SIZE }
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}
