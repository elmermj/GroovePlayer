package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class PreviousSongUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun previous() = repo.previous()
}