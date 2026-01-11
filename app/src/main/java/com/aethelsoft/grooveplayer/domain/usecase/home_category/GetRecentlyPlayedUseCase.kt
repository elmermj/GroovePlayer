package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentlyPlayedUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    operator fun invoke(limit: Int = 50): Flow<List<Song>> {
        return playbackHistoryRepository.getRecentlyPlayed(limit)
    }
}

