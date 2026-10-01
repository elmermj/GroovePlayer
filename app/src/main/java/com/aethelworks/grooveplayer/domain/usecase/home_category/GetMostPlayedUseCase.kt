package com.aethelworks.grooveplayer.domain.usecase.home_category

import com.aethelworks.grooveplayer.domain.model.MostPlayedTrack
import com.aethelworks.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Tracks ranked by local playback-history play count, highest first.
 */
class GetMostPlayedUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository,
) {
    operator fun invoke(limit: Int = 50): Flow<List<MostPlayedTrack>> {
        return playbackHistoryRepository.getMostPlayed(limit)
    }
}
