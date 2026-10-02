package com.aethelworks.grooveplayer.domain.usecase.auth_category

import com.aethelworks.grooveplayer.domain.auth.SessionLoadPurpose
import com.aethelworks.grooveplayer.domain.model.AuthUser
import com.aethelworks.grooveplayer.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreAuthSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        boundByStartupTimeout: Boolean = true,
        purpose: SessionLoadPurpose = SessionLoadPurpose.FOLLOW_UP,
    ): Result<AuthUser?> = authRepository.restoreSession(boundByStartupTimeout, purpose)
}
