package com.aethelsoft.grooveplayer.domain.usecase.equalizer_category

import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import javax.inject.Inject

/**
 * UseCase for enabling/disabling equalizer.
 */
class SetEqualizerEnabledUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        equalizerRepository.setEnabled(enabled)
    }
}
