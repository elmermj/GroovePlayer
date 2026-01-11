package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class NextSongUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun next() = repo.next()
}