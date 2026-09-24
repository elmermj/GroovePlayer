package com.aethelsoft.grooveplayer.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.backup.LibraryRestoreGate
import com.aethelsoft.grooveplayer.domain.backup.ColdStartAction
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.RestoreCloudLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class RestoreApplyUiState(
    val status: String = "Applying restored data…",
    val busy: Boolean = true,
    val error: String? = null,
    val finished: Boolean = false,
)

@HiltViewModel
class RestoreLaunchViewModel @Inject constructor(
    gate: LibraryRestoreGate,
) : ViewModel() {
    val startOnApplyScreen: Boolean = gate.launchAction == ColdStartAction.RESUME_APPLY
}

@HiltViewModel
class RestoreApplyViewModel @Inject constructor(
    private val restoreCloudLibraryUseCase: RestoreCloudLibraryUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(RestoreApplyUiState())
    val ui: StateFlow<RestoreApplyUiState> = _ui.asStateFlow()

    private val started = AtomicBoolean(false)

    fun run(startDownload: Boolean) {
        if (!started.compareAndSet(false, true)) return
        viewModelScope.launch {
            if (startDownload) {
                _ui.value = RestoreApplyUiState(status = "Downloading restored data…")
                val staged = restoreCloudLibraryUseCase.stage()
                if (staged.isFailure) {
                    _ui.value = failed(staged.exceptionOrNull())
                    return@launch
                }
            }
            _ui.value = RestoreApplyUiState(status = "Applying restored data…")
            val applied = restoreCloudLibraryUseCase.apply()
            if (applied.isFailure) {
                _ui.value = failed(applied.exceptionOrNull())
                return@launch
            }
            _ui.value = RestoreApplyUiState(
                status = "Applying restored data…",
                busy = false,
                finished = true,
            )
        }
    }

    private fun failed(error: Throwable?): RestoreApplyUiState {
        return RestoreApplyUiState(
            status = "Couldn't apply the restored library",
            busy = false,
            error = error?.message?.takeIf { it.isNotBlank() }
                ?: "Your downloaded songs were not changed.",
        )
    }
}
