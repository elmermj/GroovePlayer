package com.aethelworks.grooveplayer.domain.usecase.auth_category

import android.app.Activity
import com.aethelworks.grooveplayer.domain.model.AuthUser
import com.aethelworks.grooveplayer.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(activity: Activity): Result<AuthUser> =
        authRepository.signInWithGoogle(activity)
}
