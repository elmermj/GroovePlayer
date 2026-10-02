package com.aethelworks.grooveplayer.presentation.restore_apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.data.backup.LibraryRestoreGate
import com.aethelworks.grooveplayer.domain.backup.BackupProgressLine
import com.aethelworks.grooveplayer.domain.backup.ColdStartAction
import com.aethelworks.grooveplayer.domain.backup.RestoreProgressLabel
import com.aethelworks.grooveplayer.domain.backup.RestoreProgressSnapshot
import com.aethelworks.grooveplayer.domain.backup.RestoreUiPhase
import com.aethelworks.grooveplayer.domain.backup.TransferCancel
import com.aethelworks.grooveplayer.domain.usecase.backup_category.RestoreCloudLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class RestoreApplyUiState(
    val status: String = RestoreProgressLabel.PREPARING,
    val retry: String? = null,
    val detail: String? = null,
    /** Determinate 0f..1f once download work is known. Null shows an indeterminate spinner. */
    val fraction: Float? = null,
    val busy: Boolean = true,
    val error: String? = null,
    val finished: Boolean = false,
    val cancelled: Boolean = false,
    val canCancel: Boolean = true,
    val phaseLines: List<BackupProgressLine> = emptyList(),
)

@HiltViewModel
class RestoreLaunchViewModel @Inject constructor(
    gate: LibraryRestoreGate,
) : ViewModel() {
    val launchAction: ColdStartAction = gate.launchAction
    val startOnApplyScreen: Boolean =
        launchAction == ColdStartAction.RESUME_APPLY ||
            launchAction == ColdStartAction.RESUME_DOWNLOAD
    val resumeDownload: Boolean = launchAction == ColdStartAction.RESUME_DOWNLOAD
}

@HiltViewModel
class RestoreApplyViewModel @Inject constructor(
    private val restoreCloudLibraryUseCase: RestoreCloudLibraryUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(RestoreApplyUiState())
    val ui: StateFlow<RestoreApplyUiState> = _ui.asStateFlow()

    private val started = AtomicBoolean(false)
    private val applying = AtomicBoolean(false)
    private var job: Job? = null

    fun run(startDownload: Boolean) {
        if (!started.compareAndSet(false, true)) return
        applying.set(false)
        job = viewModelScope.launch {
            try {
                runRestore(startDownload)
            } catch (e: CancellationException) {
                if (!applying.get()) {
                    withContext(NonCancellable) {
                        runCatching { restoreCloudLibraryUseCase.abandon() }
                        _ui.value = cancelledUi()
                    }
                }
                throw e
            }
        }
    }

    fun cancel() {
        if (applying.get() || _ui.value.cancelled) return
        restoreCloudLibraryUseCase.cancel()
        job?.cancel()
    }

    fun retry() {
        if (_ui.value.busy || job?.isActive == true) return
        started.set(false)
        run(startDownload = true)
    }

    private suspend fun runRestore(startDownload: Boolean) {
        if (startDownload) {
            val preparing = RestoreProgressSnapshot(
                phase = RestoreUiPhase.PREPARING,
                status = RestoreProgressLabel.PREPARING,
            )
            var last = preparing
            _ui.value = preparing.toUi()
            val downloadFailed = coroutineScope {
                val collect = launch {
                    restoreCloudLibraryUseCase.observeProgress().collect { snap ->
                        if (applying.get()) return@collect
                        last = snap
                        _ui.value = snap.toUi()
                    }
                }
                val staged = try {
                    restoreCloudLibraryUseCase.stage()
                } catch (e: CancellationException) {
                    collect.cancelAndJoin()
                    throw e
                }
                collect.cancelAndJoin()
                if (staged.isFailure) {
                    _ui.value = failed(staged.exceptionOrNull(), last, downloading = true)
                    true
                } else {
                    false
                }
            }
            if (downloadFailed) return
        }
        val finishing = RestoreProgressSnapshot(
            phase = RestoreUiPhase.APPLYING,
            status = RestoreProgressLabel.APPLYING,
            fraction = 1f,
        )
        _ui.value = finishing.toUi().copy(canCancel = true)
        delay(RestoreProgressLabel.MIN_APPLYING_VISIBLE_MS)
        applying.set(true)
        _ui.value = _ui.value.copy(canCancel = false)
        val applied = restoreCloudLibraryUseCase.apply()
        if (applied.isFailure) {
            applying.set(false)
            _ui.value = failed(applied.exceptionOrNull(), finishing, downloading = false)
            return
        }
        _ui.value = finishing.toUi().copy(
            busy = false,
            canCancel = false,
            finished = true,
        )
    }

    private fun RestoreProgressSnapshot.toUi(): RestoreApplyUiState {
        return RestoreApplyUiState(
            status = status,
            retry = retry,
            detail = detail,
            fraction = fraction,
            busy = true,
            canCancel = phase != RestoreUiPhase.APPLYING,
            phaseLines = RestoreProgressLabel.phaseLines(this),
        )
    }

    private fun failed(
        error: Throwable?,
        last: RestoreProgressSnapshot,
        downloading: Boolean,
    ): RestoreApplyUiState {
        return RestoreApplyUiState(
            status = if (downloading) {
                "Couldn't download the restored library"
            } else {
                "Couldn't apply the restored library"
            },
            retry = last.retry,
            detail = last.detail,
            fraction = last.fraction,
            busy = false,
            canCancel = false,
            error = error?.message?.takeIf { it.isNotBlank() }
                ?: "Your downloaded songs were not changed.",
            phaseLines = RestoreProgressLabel.phaseLines(last, failed = true),
        )
    }

    private fun cancelledUi(): RestoreApplyUiState {
        return RestoreApplyUiState(
            status = TransferCancel.RESTORE_CANCELLED,
            busy = false,
            cancelled = true,
            canCancel = false,
        )
    }
}
