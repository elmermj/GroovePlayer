package com.aethelsoft.grooveplayer.domain.usecase.auth_category

import android.app.Activity
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(activity: Activity): Result<AuthUser> =
        authRepository.signInWithGoogle(activity)
}
