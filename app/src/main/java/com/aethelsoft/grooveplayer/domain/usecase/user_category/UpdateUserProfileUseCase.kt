package com.aethelsoft.grooveplayer.domain.usecase.user_category

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import javax.inject.Inject

/**
 * UseCase for updating specific user_category profile fields.
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend fun updateUsername(username: String) {
        userRepository.updateUsername(username)
    }
    
    suspend fun updateEmail(email: String) {
        userRepository.updateEmail(email)
    }
    
    suspend fun updateProfilePicture(url: String?) {
        userRepository.updateProfilePicture(url)
    }
    
    suspend fun updatePrivilegeTier(tier: PrivilegeTier) {
        userRepository.updatePrivilegeTier(tier)
    }
}
