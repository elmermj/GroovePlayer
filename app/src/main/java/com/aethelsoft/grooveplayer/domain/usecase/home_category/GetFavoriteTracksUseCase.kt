package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.SongLikeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Liked songs, newest like first. [sinceTimestamp] is unused; likes are not a play window.
 */
class GetFavoriteTracksUseCase @Inject constructor(
    private val songLikeRepository: SongLikeRepository,
) {
    @Suppress("UNUSED_PARAMETER")
    operator fun invoke(sinceTimestamp: Long, limit: Int = 50): Flow<List<Song>> {
        return songLikeRepository.observeFavoriteTracks(limit)
    }
}

