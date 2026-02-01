package com.aethelsoft.grooveplayer.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aethelsoft.grooveplayer.domain.model.Song

/** PagingSource that always returns an empty page. Used when no filter (e.g. artist/album) is set. */
class EmptySongsPagingSource : PagingSource<Int, Song>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        return LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )
    }
    override fun getRefreshKey(state: PagingState<Int, Song>): Int? = null
}
