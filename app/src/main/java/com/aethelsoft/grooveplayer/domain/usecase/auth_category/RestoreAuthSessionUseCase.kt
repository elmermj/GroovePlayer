package com.aethelsoft.grooveplayer.domain.usecase.auth_category

import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreAuthSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<AuthUser?> = authRepository.restoreSession()
}
