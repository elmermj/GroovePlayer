package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import javax.inject.Inject

class GetFavoriteTracksUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(sinceTimestamp: Long, limit: Int = 50): List<Song> {
        return playbackHistoryRepository.getFavoriteTracks(sinceTimestamp, limit)
    }
}

