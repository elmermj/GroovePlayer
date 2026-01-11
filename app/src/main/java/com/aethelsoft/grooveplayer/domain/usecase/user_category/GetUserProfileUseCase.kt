package com.aethelsoft.grooveplayer.domain.usecase.user_category

import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing user_category profile reactively.
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<UserProfile?> {
        return userRepository.observeUserProfile()
    }
    
    suspend fun getOnce(): UserProfile? {
        return userRepository.getUserProfile()
    }
}
