package com.aethelsoft.grooveplayer.domain.usecase.equalizer_category

import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import javax.inject.Inject

/**
 * UseCase for saving equalizer settings to persistent storage.
 */
class SaveEqualizerSettingsUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    suspend operator fun invoke() {
        equalizerRepository.saveSettings()
    }
}
