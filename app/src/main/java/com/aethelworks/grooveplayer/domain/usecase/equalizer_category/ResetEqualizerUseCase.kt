package com.aethelworks.grooveplayer.domain.usecase.equalizer_category

import com.aethelworks.grooveplayer.domain.repository.EqualizerRepository
import javax.inject.Inject

/**
 * UseCase for resetting equalizer to flat (all bands at 0).
 */
class ResetEqualizerUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    suspend operator fun invoke() {
        equalizerRepository.reset()
    }
}
