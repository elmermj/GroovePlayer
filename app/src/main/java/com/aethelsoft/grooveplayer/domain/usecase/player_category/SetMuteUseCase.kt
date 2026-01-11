package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class SetMuteUseCase @Inject constructor(private val repository: PlayerRepository){
    suspend fun setMute(mute: Boolean) = repository.setMute(mute)
}