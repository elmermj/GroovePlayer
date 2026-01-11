package com.aethelsoft.grooveplayer.domain.usecase.user_category

import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import javax.inject.Inject

/**
 * UseCase for saving or updating user_category profile.
 */
class SaveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(profile: UserProfile) {
        userRepository.saveUserProfile(profile)
    }
}
