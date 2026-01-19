package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for getting last played songs reactively.
 * Returns a Flow that updates in real-time when playback history changes.
 */
class GetLastPlayedSongsUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    operator fun invoke(sinceTimestamp: Long, limit: Int = 8): Flow<List<Song>> {
        return playbackHistoryRepository.getLastPlayedSongs(sinceTimestamp, limit)
    }
}