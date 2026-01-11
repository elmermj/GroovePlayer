package com.aethelsoft.grooveplayer.domain.usecase.player_category

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class SetVolumeUseCase @Inject constructor(private val repository: PlayerRepository){
    suspend fun setVolume(volume: Float) = repository.setVolume(volume)

}