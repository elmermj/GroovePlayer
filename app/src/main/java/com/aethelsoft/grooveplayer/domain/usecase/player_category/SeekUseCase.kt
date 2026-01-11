package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class SeekUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(positionMs: Long) = repo.seekTo(positionMs)
}