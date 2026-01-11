package com.aethelsoft.grooveplayer.domain.usecase.settings_category

import jakarta.inject.Inject

class InitializeNewUserSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke(){
        userRepository.initializeNewUserSettings()
    }
}