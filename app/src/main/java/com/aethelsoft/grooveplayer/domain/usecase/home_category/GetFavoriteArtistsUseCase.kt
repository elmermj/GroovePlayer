package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for getting favorite artists reactively.
 * Returns a Flow that updates in real-time when playback history changes.
 */
class GetFavoriteArtistsUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    operator fun invoke(sinceTimestamp: Long, limit: Int = 50): Flow<List<FavoriteArtist>> {
        return playbackHistoryRepository.getFavoriteArtists(sinceTimestamp, limit)
    }
}

