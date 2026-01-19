package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for getting favorite tracks reactively.
 * Returns a Flow that updates in real-time when playback history changes.
 */
class GetFavoriteTracksUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    operator fun invoke(sinceTimestamp: Long, limit: Int = 50): Flow<List<Song>> {
        return playbackHistoryRepository.getFavoriteTracks(sinceTimestamp, limit)
    }
}

