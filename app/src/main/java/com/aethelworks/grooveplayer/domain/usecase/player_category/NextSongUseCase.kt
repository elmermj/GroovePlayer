package com.aethelworks.grooveplayer.domain.usecase.player_category

import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class NextSongUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun next() = repo.next()
}