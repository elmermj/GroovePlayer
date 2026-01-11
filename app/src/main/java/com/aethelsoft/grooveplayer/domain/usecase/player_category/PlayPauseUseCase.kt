package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class PlayPauseUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun play() = repo.play()
    suspend fun pause() = repo.pause()
}