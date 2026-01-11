package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.repository.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import javax.inject.Inject

class GetFavoriteArtistsUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(sinceTimestamp: Long, limit: Int = 50): List<FavoriteArtist> {
        return playbackHistoryRepository.getFavoriteArtists(sinceTimestamp, limit)
    }
}

