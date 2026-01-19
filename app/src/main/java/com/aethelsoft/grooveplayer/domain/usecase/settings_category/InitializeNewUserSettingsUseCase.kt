package com.aethelsoft.grooveplayer.domain.usecase.settings_category

import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import javax.inject.Inject

/**
 * UseCase for initializing new user settings.
 * Creates default settings if they don't exist.
 */
class InitializeNewUserSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() {
        // This will initialize settings with defaults if they don't exist
        userRepository.getUserSettings()
    }
}