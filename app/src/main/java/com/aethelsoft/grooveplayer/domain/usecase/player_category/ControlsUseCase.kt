package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class ControlsUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend fun setShuffle(enabled: Boolean) = repo.setShuffle(enabled)
    suspend fun setRepeat(mode: RepeatMode) = repo.setRepeat(mode)
}