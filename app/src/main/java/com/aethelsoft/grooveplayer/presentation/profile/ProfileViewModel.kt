package com.aethelsoft.grooveplayer.presentation.profile

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.model.UserSettings
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetMusicFolderPathsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetStorageUsageUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetUserProfileUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetUserSettingsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.UpdateUserSettingsUseCase
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aethelsoft.grooveplayer.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val userRepository: UserRepository,
    private val getMusicFolderPathsUseCase: GetMusicFolderPathsUseCase,
    private val getStorageUsageUseCase: GetStorageUsageUseCase,
) : BaseViewModel(application){

    val excludedFolders: StateFlow<List<String>> = getUserSettingsUseCase()
        .map { it.excludedFolders }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _folderSuggestions = MutableStateFlow<List<String>>(emptyList())
    val folderSuggestions: StateFlow<List<String>> = _folderSuggestions.asStateFlow()

    private val _storageUsage = MutableStateFlow<StorageUsageData?>(null)
    val storageUsage: StateFlow<StorageUsageData?> = _storageUsage.asStateFlow()

    private val _isStorageLoading = MutableStateFlow(false)
    val isStorageLoading: StateFlow<Boolean> = _isStorageLoading.asStateFlow()

    val userProfile: StateFlow<UserProfile?> =
        getUserProfileUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userSettings: StateFlow<UserSettings> =
        getUserSettingsUseCase()
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                UserSettings()
            )

    val isMiniPlayerOnStartEnabled: StateFlow<Boolean> = getUserSettingsUseCase()
        .map { it.showMiniPlayerOnStart }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun refresh() {
        setSuccess(Unit)
    }

    fun setFadeTimer(seconds: Int) = viewModelScope.launch {
        updateUserSettingsUseCase.updateFadeTimer(seconds)
    }

    fun setVisualizationMode(mode: VisualizationMode) = viewModelScope.launch {
        userRepository.updateVisualizationMode(mode)
    }

    fun setMiniPlayerOnStartEnabled(enabled: Boolean) = viewModelScope.launch {
        userRepository.updateShowMiniPlayerOnStart(enabled)
    }

    fun loadFolderSuggestions() = viewModelScope.launch {
        _folderSuggestions.value = getMusicFolderPathsUseCase()
    }

    fun excludeFolder(path: String) = viewModelScope.launch {
        val current = userRepository.getUserSettings().excludedFolders
        if (path !in current) {
            userRepository.updateExcludedFolders(current + path)
            _storageUsage.value = null  // Invalidate cache
        }
    }

    fun includeFolder(path: String) = viewModelScope.launch {
        val current = userRepository.getUserSettings().excludedFolders
        userRepository.updateExcludedFolders(current - path)
        _storageUsage.value = null  // Invalidate cache
    }

    /** Loads storage usage only when row is expanded. Uses cache if available. */
    fun loadStorageUsage() = viewModelScope.launch {
        if (_storageUsage.value != null) return@launch  // Use cached
        _isStorageLoading.value = true
        _storageUsage.value = try {
            getStorageUsageUseCase()
        } catch (e: Exception) {
            null
        }
        _isStorageLoading.value = false
    }
}