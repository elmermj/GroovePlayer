package com.aethelworks.grooveplayer.domain.usecase.equalizer_category

import com.aethelworks.grooveplayer.domain.model.EqualizerState
import com.aethelworks.grooveplayer.domain.repository.EqualizerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing equalizer state reactively.
 */
class ObserveEqualizerStateUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    operator fun invoke(): Flow<EqualizerState> {
        return equalizerRepository.observeEqualizerState()
    }
}
