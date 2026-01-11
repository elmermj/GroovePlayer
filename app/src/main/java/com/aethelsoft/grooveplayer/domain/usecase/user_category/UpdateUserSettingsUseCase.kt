package com.aethelsoft.grooveplayer.domain.usecase.user_category

import com.aethelsoft.grooveplayer.domain.model.UserSettings
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import javax.inject.Inject

/**
 * UseCase for updating user_category settings.
 */
class UpdateUserSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(settings: UserSettings) {
        userRepository.saveUserSettings(settings)
    }
    
    suspend fun updateLastPlayedSongsTimer(days: Int) {
        userRepository.updateLastPlayedSongsTimer(days)
    }
    
    suspend fun updateFadeTimer(seconds: Int) {
        userRepository.updateFadeTimer(seconds)
    }
    
    suspend fun resetToDefault() {
        userRepository.resetSettingsToDefault()
    }
}
