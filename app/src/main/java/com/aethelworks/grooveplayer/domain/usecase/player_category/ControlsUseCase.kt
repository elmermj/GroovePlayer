package com.aethelworks.grooveplayer.domain.usecase.player_category

import com.aethelworks.grooveplayer.domain.model.RepeatMode
import com.aethelworks.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class ControlsUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun setShuffle(enabled: Boolean, reorderQueue: Boolean = true) = repo.setShuffle(enabled, reorderQueue)
    suspend fun setRepeat(mode: RepeatMode) = repo.setRepeat(mode)
}