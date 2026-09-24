package com.aethelsoft.grooveplayer.presentation.backup

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.model.CloudBackupState
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelsoft.grooveplayer.domain.model.TrimStrategy
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.usecase.auth_category.RestoreAuthSessionUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.DeleteBackupObjectUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.FetchCloudLibraryUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.GetIncludedBackupFoldersUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.ListBackupObjectsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.ObserveCloudBackupStateUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.StartCloudBackupUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.TrimCloudBackupUseCase
import com.aethelsoft.grooveplayer.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    application: Application,
    observeCloudBackupStateUseCase: ObserveCloudBackupStateUseCase,
    private val startCloudBackupUseCase: StartCloudBackupUseCase,
    private val getIncludedBackupFoldersUseCase: GetIncludedBackupFoldersUseCase,
    private val listBackupObjectsUseCase: ListBackupObjectsUseCase,
    private val deleteBackupObjectUseCase: DeleteBackupObjectUseCase,
    private val trimCloudBackupUseCase: TrimCloudBackupUseCase,
    private val fetchCloudLibraryUseCase: FetchCloudLibraryUseCase,
    private val authRepository: AuthRepository,
    private val restoreAuthSessionUseCase: RestoreAuthSessionUseCase,
) : BaseViewModel(application) {

    val backupState: StateFlow<CloudBackupState> =
        observeCloudBackupStateUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, CloudBackupState())

    val authUser: StateFlow<AuthUser?> =
        authRepository.observeAuthUser()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val privilegeTier: StateFlow<PrivilegeTier> =
        authRepository.observePrivilegeTier()
            .stateIn(viewModelScope, SharingStarted.Eagerly, PrivilegeTier.FREE)

    private val _includedFolders = MutableStateFlow<List<String>>(emptyList())
    val includedFolders: StateFlow<List<String>> = _includedFolders.asStateFlow()

    private val _rawObjects = MutableStateFlow<List<BackupObject>>(emptyList())
    private val _objectsFilter = MutableStateFlow(BackupObjectsFilter.ALL)
    val objectsFilter: StateFlow<BackupObjectsFilter> = _objectsFilter.asStateFlow()

    val objects: StateFlow<List<BackupObject>> =
        combine(_rawObjects, _objectsFilter) { list, filter -> filter.apply(list) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _objectsLoading = MutableStateFlow(false)
    val objectsLoading: StateFlow<Boolean> = _objectsLoading.asStateFlow()

    /** True while PTR / onResume / open is refetching /v1/me + objects + library. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val refreshMutex = Mutex()

    private val _objectsError = MutableStateFlow<String?>(null)
    val objectsError: StateFlow<String?> = _objectsError.asStateFlow()

    private val _deleteInFlight = MutableStateFlow(false)
    val deleteInFlight: StateFlow<Boolean> = _deleteInFlight.asStateFlow()

    private val _trimInFlight = MutableStateFlow(false)
    val trimInFlight: StateFlow<Boolean> = _trimInFlight.asStateFlow()

    private val _trimMessage = MutableStateFlow<String?>(null)
    val trimMessage: StateFlow<String?> = _trimMessage.asStateFlow()

    private val _lastTrimResult = MutableStateFlow<TrimCloudBackupResult?>(null)
    val lastTrimResult: StateFlow<TrimCloudBackupResult?> = _lastTrimResult.asStateFlow()

    /** Null until first room_db upload — Restore CTA disabled/empty. */
    private val _librarySnapshot = MutableStateFlow<CloudLibrarySnapshot?>(null)
    val librarySnapshot: StateFlow<CloudLibrarySnapshot?> = _librarySnapshot.asStateFlow()

    private val _libraryLoading = MutableStateFlow(false)
    val libraryLoading: StateFlow<Boolean> = _libraryLoading.asStateFlow()

    private val _restoreInFlight = MutableStateFlow(false)
    val restoreInFlight: StateFlow<Boolean> = _restoreInFlight.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthUser().collect { user ->
                if (user == null) {
                    _rawObjects.value = emptyList()
                    _objectsError.value = null
                    _trimMessage.value = null
                    _lastTrimResult.value = null
                    _librarySnapshot.value = null
                    _restoreMessage.value = null
                }
            }
        }
        // Same live /v1/me + objects path as PTR / onResume / Backup open.
        refreshEntitlements()
    }

    fun refreshIncludedFolders() = viewModelScope.launch {
        _includedFolders.value = getIncludedBackupFoldersUseCase()
    }

    fun startBackup() = viewModelScope.launch {
        startCloudBackupUseCase()
        _includedFolders.value = getIncludedBackupFoldersUseCase()
        loadObjects()
        loadLibrarySnapshot()
    }

    /**
     * Live refetch of `/v1/me` (tier + storage) then replace backed-up songs +
     * library snapshot from the backup APIs. Used by PTR, onResume, and Backup open.
     * Replaces in-memory state on success — no stale Premium/usage/list after purge.
     */
    fun refreshEntitlements() = viewModelScope.launch {
        if (!refreshMutex.tryLock()) return@launch
        _isRefreshing.value = true
        try {
            // Always hit network /v1/me (restoreSession) so post-purge Free + used=0 apply
            // without process death.
            restoreAuthSessionUseCase().onFailure { e ->
                _objectsError.value = e.message?.takeIf { it.isNotBlank() }
                    ?: "Can't reach server. Check Wi‑Fi or API URL, then retry."
            }
            _includedFolders.value = getIncludedBackupFoldersUseCase()
            loadObjectsInternal()
            loadLibrarySnapshotInternal()
        } finally {
            _isRefreshing.value = false
            refreshMutex.unlock()
        }
    }

    fun setObjectsFilter(filter: BackupObjectsFilter) {
        _objectsFilter.value = filter
    }

    fun loadObjects() = viewModelScope.launch {
        loadObjectsInternal()
    }

    private suspend fun loadObjectsInternal() {
        if (!authRepository.isSignedIn()) {
            _rawObjects.value = emptyList()
            _objectsError.value = null
            _objectsLoading.value = false
            return
        }
        _objectsLoading.value = true
        _objectsError.value = null
        try {
            listBackupObjectsUseCase()
                .onSuccess { _rawObjects.value = it }
                .onFailure {
                    _objectsError.value = it.message ?: "Could not load cloud backups"
                }
        } finally {
            _objectsLoading.value = false
        }
    }

    fun loadLibrarySnapshot() = viewModelScope.launch {
        loadLibrarySnapshotInternal()
    }

    private suspend fun loadLibrarySnapshotInternal() {
        if (!authRepository.isSignedIn()) {
            _librarySnapshot.value = null
            return
        }
        _libraryLoading.value = true
        try {
            fetchCloudLibraryUseCase()
                .onSuccess { _librarySnapshot.value = it }
                .onFailure {
                    // Keep prior snapshot; surface soft message only.
                    _restoreMessage.value = it.message
                }
        } finally {
            _libraryLoading.value = false
        }
    }

    fun deleteObject(obj: BackupObject) = viewModelScope.launch {
        _deleteInFlight.value = true
        try {
            deleteBackupObjectUseCase(obj.id)
                .onSuccess {
                    _rawObjects.value = _rawObjects.value.filterNot { it.id == obj.id }
                    restoreAuthSessionUseCase()
                    loadLibrarySnapshot()
                }
                .onFailure {
                    _objectsError.value = it.message ?: "Could not remove from cloud backup"
                }
        } finally {
            _deleteInFlight.value = false
        }
    }

    fun trimCloud(
        strategy: TrimStrategy,
        artist: String? = null,
        album: String? = null,
        year: Int? = null,
        limitBytes: Long? = null,
    ) = viewModelScope.launch {
        _trimInFlight.value = true
        _trimMessage.value = null
        try {
            val result = trimCloudBackupUseCase(
                TrimCloudBackupRequest(
                    strategy = strategy,
                    artist = artist,
                    album = album,
                    year = year,
                    limitBytes = limitBytes,
                )
            )
            result.onSuccess {
                _lastTrimResult.value = it
                _trimMessage.value =
                    "Removed ${it.deleted} whole song(s) from cloud backup · freed ${it.bytesFreed} bytes"
                restoreAuthSessionUseCase()
                loadObjects()
                loadLibrarySnapshot()
            }.onFailure {
                _trimMessage.value = it.message ?: "Could not free cloud space"
            }
        } finally {
            _trimInFlight.value = false
        }
    }

    fun clearTrimMessage() {
        _trimMessage.value = null
    }

    override fun refresh() {
        setSuccess(Unit)
        refreshEntitlements()
    }
}
