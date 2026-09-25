package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.repository.SongLikeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Albums that still have at least one liked track. [sinceTimestamp] is unused.
 */
class GetFavoriteAlbumsUseCase @Inject constructor(
    private val songLikeRepository: SongLikeRepository,
) {
    @Suppress("UNUSED_PARAMETER")
    operator fun invoke(sinceTimestamp: Long, limit: Int = 50): Flow<List<FavoriteAlbum>> {
        return songLikeRepository.observeFavoriteAlbums(limit)
    }
}

