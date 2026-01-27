package com.aethelsoft.grooveplayer.domain.usecase.equalizer_category

import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import javax.inject.Inject

/**
 * UseCase for setting equalizer band level.
 */
class SetEqualizerBandLevelUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    suspend operator fun invoke(band: Int, level: Int) {
        equalizerRepository.setBandLevel(band, level)
    }
}
