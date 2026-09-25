package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.repository.SongLikeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Artists that still have at least one liked track. [sinceTimestamp] is unused.
 */
class GetFavoriteArtistsUseCase @Inject constructor(
    private val songLikeRepository: SongLikeRepository,
) {
    @Suppress("UNUSED_PARAMETER")
    operator fun invoke(sinceTimestamp: Long, limit: Int = 50): Flow<List<FavoriteArtist>> {
        return songLikeRepository.observeFavoriteArtists(limit)
    }
}

