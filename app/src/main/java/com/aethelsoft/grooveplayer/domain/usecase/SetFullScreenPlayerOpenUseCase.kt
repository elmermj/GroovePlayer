package com.aethelsoft.grooveplayer.domain.usecase

import com.aethelsoft.grooveplayer.domain.repository.PlayerRepository
import javax.inject.Inject

class SetFullScreenPlayerOpenUseCase @Inject constructor(private val repository: PlayerRepository){
    suspend fun setFullScreenPlayerOpen(isOpen: Boolean) = repository.setFullScreenPlayerOpen(isOpen)
}