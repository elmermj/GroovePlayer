package com.aethelsoft.grooveplayer.domain.usecase.equalizer_category

import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import javax.inject.Inject

/**
 * UseCase for setting equalizer preset.
 */
class SetEqualizerPresetUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    suspend operator fun invoke(preset: Int) {
        equalizerRepository.setPreset(preset)
    }
}
