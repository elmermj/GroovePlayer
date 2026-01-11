package com.aethelsoft.grooveplayer.domain.usecase.home_category

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.repository.PlaybackHistoryRepository
import javax.inject.Inject

class GetFavoriteAlbumsUseCase @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(sinceTimestamp: Long, limit: Int = 50): List<FavoriteAlbum> {
        return playbackHistoryRepository.getFavoriteAlbums(sinceTimestamp, limit)
    }
}

