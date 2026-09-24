package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

/** In-place queue edits for the queue sheet: jump, reorder, remove, undo-remove. */
class EditQueueUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun skipTo(index: Int) = repo.skipToQueueIndex(index)
    suspend fun move(from: Int, to: Int) = repo.moveQueueItem(from, to)
    suspend fun remove(index: Int): Boolean = repo.removeQueueItem(index)
    suspend fun insert(index: Int, song: Song) = repo.insertQueueItem(index, song)
    suspend fun playNext(song: Song) = repo.playNext(song)
}
