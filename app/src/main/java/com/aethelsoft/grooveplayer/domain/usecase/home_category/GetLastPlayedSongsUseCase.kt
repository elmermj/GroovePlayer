package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import jakarta.inject.Inject

class GetLastPlayedSongsUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(sinceTimestamp: Long, limit: Int = 8): List<Song> {
        return playbackHistoryRepository.getLastPlayedSongs(sinceTimestamp, limit)
    }
}