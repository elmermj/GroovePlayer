package com.aethelsoft.grooveplayer.presentation.profile

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.model.UserSettings
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.domain.model.StorageUsageData
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import com.aethelsoft.grooveplayer.domain.usecase.home_category.RefreshMusicCatalogUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetMusicFolderPathsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetStorageUsageUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetUserProfileUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.GetUserSettingsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.user_category.UpdateUserSettingsUseCase
import com.aethelsoft.grooveplayer.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository,
    private val getMusicFolderPathsUseCase: GetMusicFolderPathsUseCase,
    private val getStorageUsageUseCase: GetStorageUsageUseCase,
    private val refreshMusicCatalogUseCase: RefreshMusicCatalogUseCase,
) : BaseViewModel(application) {

    /** Draft exclusions edited in-session; committed when Profile closes. */
    private val _pendingExcludedFolders = MutableStateFlow<List<String>>(emptyList())
    val excludedFolders: StateFlow<List<String>> = _pendingExcludedFolders.asStateFlow()

    private var committedExcludedFolders: List<String> = emptyList()
    private val commitMutex = Mutex()

    private val _folderSuggestions = MutableStateFlow<List<String>>(emptyList())
    val folderSuggestions: StateFlow<List<String>> = _folderSuggestions.asStateFlow()

    private val _storageUsage = MutableStateFlow<StorageUsageData?>(null)
    val storageUsage: StateFlow<StorageUsageData?> = _storageUsage.asStateFlow()

    private val _isStorageLoading = MutableStateFlow(false)
    val isStorageLoading: StateFlow<Boolean> = _isStorageLoading.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    private val _activeRowId = MutableStateFlow<String?>(null)
    val activeRowId: StateFlow<String?> = _activeRowId.asStateFlow()

    private val _storageActiveRowId = MutableStateFlow<String?>(null)
    val storageActiveRowId: StateFlow<String?> = _storageActiveRowId.asStateFlow()

    fun setActiveRowId(id: String?) {
        _activeRowId.value = id
    }

    fun setStorageActiveRowId(id: String?) {
        _storageActiveRowId.value = id
    }

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

    val isNotificationsEnabled: StateFlow<Boolean> = getUserSettingsUseCase()
        .map { it.notificationsEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch {
            val initial = userRepository.getUserSettings().excludedFolders
            committedExcludedFolders = initial
            _pendingExcludedFolders.value = initial
        }
    }

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

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        userRepository.updateNotificationsEnabled(enabled)
    }

    fun loadFolderSuggestions() = viewModelScope.launch {
        _folderSuggestions.value = getMusicFolderPathsUseCase()
    }

    fun excludeFolder(path: String) {
        val current = _pendingExcludedFolders.value
        if (path !in current) {
            _pendingExcludedFolders.value = current + path
            _storageUsage.value = null
        }
    }

    fun includeFolder(path: String) {
        _pendingExcludedFolders.value = _pendingExcludedFolders.value - path
        _storageUsage.value = null
    }

    /**
     * Persists draft excluded folders and refreshes catalogs.
     * Safe to call multiple times (no-op when unchanged / already committing).
     */
    fun commitPendingExcludedFolders() {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            commitPendingExcludedFoldersInternal()
        }
    }

    private suspend fun commitPendingExcludedFoldersInternal() = commitMutex.withLock {
        val pending = _pendingExcludedFolders.value
        if (pending.toSet() == committedExcludedFolders.toSet()) return

        val beforeIds = musicRepository.getAllSongs().map { it.id }.toSet()
        userRepository.updateExcludedFolders(pending)
        committedExcludedFolders = pending
        val afterIds = musicRepository.getAllSongs().map { it.id }.toSet()
        // Refresh for both newly excluded (before - after) and newly included (after - before) folders.
        refreshMusicCatalogUseCase(removedSongIds = beforeIds - afterIds)
    }

    /** Loads storage usage only when row is expanded. Uses cache if available. */
    fun loadStorageUsage() = viewModelScope.launch {
        if (_storageUsage.value != null) return@launch
        _isStorageLoading.value = true
        _storageUsage.value = try {
            getStorageUsageUseCase()
        } catch (e: Exception) {
            null
        }
        _isStorageLoading.value = false
    }

    fun refreshCacheSize() = viewModelScope.launch(Dispatchers.IO) {
        _cacheSizeBytes.value = calculateCacheSizeBytes()
    }

    fun clearAppCache() = viewModelScope.launch(Dispatchers.IO) {
        if (_isClearingCache.value) return@launch
        _isClearingCache.value = true
        try {
            deleteDirectoryContents(getApplication<Application>().cacheDir)
            getApplication<Application>().externalCacheDir?.let { deleteDirectoryContents(it) }
            _cacheSizeBytes.value = calculateCacheSizeBytes()
        } finally {
            _isClearingCache.value = false
        }
    }

    private fun calculateCacheSizeBytes(): Long {
        val app = getApplication<Application>()
        return directorySize(app.cacheDir) + (app.externalCacheDir?.let { directorySize(it) } ?: 0L)
    }

    private fun directorySize(dir: java.io.File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun deleteDirectoryContents(dir: java.io.File) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
    }

    override fun onCleared() {
        // Phone Profile route: ensure draft is committed even if UI dispose raced.
        runBlocking {
            withContext(Dispatchers.IO) {
                commitPendingExcludedFoldersInternal()
            }
        }
        super.onCleared()
    }
}
