package com.aethelsoft.grooveplayer.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.backup.LoginRestorePromptMemory
import com.aethelsoft.grooveplayer.domain.backup.BackupJobGate
import com.aethelsoft.grooveplayer.domain.backup.LoginRestorePrompt
import com.aethelsoft.grooveplayer.domain.backup.LoginRestorePromptInput
import com.aethelsoft.grooveplayer.domain.backup.RestorePhase
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase
import com.aethelsoft.grooveplayer.domain.model.CloudBackupState
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.isUploadInProgress
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.FetchCloudLibraryUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.ObserveCloudBackupStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class LoginRestorePromptViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val observeCloudBackupStateUseCase: ObserveCloudBackupStateUseCase,
    private val fetchCloudLibraryUseCase: FetchCloudLibraryUseCase,
    private val backupRepository: BackupRepository,
    private val jobGate: BackupJobGate,
    private val memory: LoginRestorePromptMemory,
) : ViewModel() {

    private val manualSurface = MutableStateFlow(false)
    private val refreshTick = MutableStateFlow(0)

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private var activeUserId: String? = null
    private var cachedSnapshot: CloudLibrarySnapshot? = null
    private var snapshotUserId: String? = null
    private var refreshedBackupAt: Long? = null
    private var sessionHandledKey: String? = null
    private var holdSessionForRevision = false
    private var pendingKey: String? = null
    private var backupUploadInProgress = false
    private var restoreInProgress = false

    init {
        viewModelScope.launch {
            combine(
                authRepository.observeAuthUser(),
                authRepository.observePrivilegeTier(),
                observeCloudBackupStateUseCase(),
                manualSurface,
                refreshTick,
            ) { user, tier, backup, onSurface, _ ->
                PromptSignals(user, tier, backup, onSurface)
            }.collect { signals ->
                evaluate(signals)
            }
        }
    }

    /** Hide the prompt while Backup or the restore progress screen is open. */
    fun onBackupRestoreVisible(visible: Boolean) {
        if (manualSurface.value != visible) manualSurface.value = visible
    }

    /**
     * User opened Restore from Backup. Forget "No" for a later session, and
     * stay quiet for the rest of this one.
     */
    fun onManualRestoreOpened() {
        memory.clearDecline()
        holdSessionForRevision = true
        refreshTick.value += 1
    }

    /** Choice A: keep the on-device library and do not start restore. */
    fun keepCurrentData() {
        val key = pendingKey ?: return
        sessionHandledKey = key
        memory.decline(key)
        pendingKey = null
        _visible.value = false
    }

    /**
     * Choice B. Returns true only when restore is free to start.
     * Does not start a second job when backup or restore is already running.
     */
    fun confirmRestore(): Boolean {
        val key = pendingKey ?: return false
        if (backupUploadInProgress || restoreInProgress) {
            _visible.value = false
            pendingKey = null
            return false
        }
        sessionHandledKey = key
        memory.decline(key)
        pendingKey = null
        _visible.value = false
        return true
    }

    private suspend fun evaluate(signals: PromptSignals) {
        val user = signals.user
        if (user == null) {
            activeUserId = null
            cachedSnapshot = null
            snapshotUserId = null
            sessionHandledKey = null
            holdSessionForRevision = false
            pendingKey = null
            backupUploadInProgress = false
            restoreInProgress = false
            _visible.value = false
            return
        }
        if (activeUserId != user.id) {
            activeUserId = user.id
            cachedSnapshot = null
            snapshotUserId = null
            sessionHandledKey = null
            holdSessionForRevision = false
            refreshedBackupAt = null
        }

        val transferBusy = jobGate.current() != null
        backupUploadInProgress = transferBusy || signals.backup.phase.isUploadInProgress()
        restoreInProgress = transferBusy || backupRepository.restorePhase() != RestorePhase.IDLE
        val premium = signals.tier == PrivilegeTier.PREMIUM
        val backupAt = signals.backup.lastBackupAtEpochMs
        val backupJustFinished = signals.backup.phase == CloudBackupPhase.SUCCESS &&
            backupAt != null &&
            backupAt != refreshedBackupAt
        val needSnapshot = premium &&
            !backupUploadInProgress &&
            !restoreInProgress &&
            (snapshotUserId != user.id || backupJustFinished)
        if (needSnapshot) {
            val fetched = fetchCloudLibraryUseCase()
            if (fetched.isSuccess) {
                if (backupJustFinished) refreshedBackupAt = backupAt
                cachedSnapshot = fetched.getOrNull()
                snapshotUserId = user.id
            }
        }

        val revision = cachedSnapshot?.let(LoginRestorePrompt::revisionOf)
        val key = revision?.let { LoginRestorePrompt.memoryKey(user.id, it) }
        if (holdSessionForRevision && key != null) {
            sessionHandledKey = key
            holdSessionForRevision = false
        }

        val offer = LoginRestorePrompt.shouldOffer(
            LoginRestorePromptInput(
                signedIn = true,
                premium = premium,
                userId = user.id,
                snapshot = cachedSnapshot,
                backupUploadInProgress = backupUploadInProgress,
                restoreInProgress = restoreInProgress,
                onBackupOrRestoreScreen = signals.onBackupOrRestoreScreen,
                persistedDeclinedKey = memory.declinedKey(),
                sessionHandledKey = sessionHandledKey,
            ),
        )
        pendingKey = offer
        _visible.value = offer != null
    }

    private data class PromptSignals(
        val user: AuthUser?,
        val tier: PrivilegeTier,
        val backup: CloudBackupState,
        val onBackupOrRestoreScreen: Boolean,
    )
}
