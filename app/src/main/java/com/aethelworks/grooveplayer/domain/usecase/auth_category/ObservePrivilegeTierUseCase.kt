package com.aethelworks.grooveplayer.domain.usecase.auth_category

import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import com.aethelworks.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePrivilegeTierUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<PrivilegeTier> = authRepository.observePrivilegeTier()
}
