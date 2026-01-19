package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class QueueUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(songs: List<Song>, startIndex: Int = 0, isEndlessQueue: Boolean = false) = repo.setQueue(songs, startIndex, isEndlessQueue)
}