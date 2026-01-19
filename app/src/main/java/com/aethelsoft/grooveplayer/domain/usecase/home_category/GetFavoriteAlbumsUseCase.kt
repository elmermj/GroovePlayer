package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for getting favorite albums reactively.
 * Returns a Flow that updates in real-time when playback history changes.
 */
class GetFavoriteAlbumsUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    operator fun invoke(sinceTimestamp: Long, limit: Int = 50): Flow<List<FavoriteAlbum>> {
        return playbackHistoryRepository.getFavoriteAlbums(sinceTimestamp, limit)
    }
}

