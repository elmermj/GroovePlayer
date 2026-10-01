package com.aethelworks.grooveplayer.domain.usecase.user_category

import com.aethelworks.grooveplayer.domain.model.UserSettings
import com.aethelworks.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing user_category settings reactively.
 */
class GetUserSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<UserSettings> {
        return userRepository.observeUserSettings()
    }
    
    suspend fun getOnce(): UserSettings {
        return userRepository.getUserSettings()
    }
}
