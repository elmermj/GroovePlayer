package com.aethelsoft.grooveplayer.data.transfer

import com.aethelsoft.grooveplayer.services.TransferServiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge for orchestrator to push transfer state to NearbyTransferService.
 * Service observes this to update its notification.
 *
 * Once [TransferServiceState.Completed] or [TransferServiceState.Failed] is set,
 * non-terminal updates are ignored so a late chunk cannot revive a stuck
 * "Transferring 99%" notification.
 */
@Singleton
class TransferNotificationBridge @Inject constructor() {

    private val _state = MutableStateFlow<TransferServiceState>(TransferServiceState.Idle)
    val state: StateFlow<TransferServiceState> = _state.asStateFlow()

    @Synchronized
    fun updateState(state: TransferServiceState) {
        val current = _state.value
        if (isTerminal(current) && !isTerminal(state) && state !is TransferServiceState.Idle) {
            return
        }
        _state.value = state
    }

    fun reset() {
        _state.value = TransferServiceState.Idle
    }

    private fun isTerminal(state: TransferServiceState): Boolean =
        state is TransferServiceState.Completed || state is TransferServiceState.Failed
}
