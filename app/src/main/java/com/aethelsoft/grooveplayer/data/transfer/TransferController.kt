package com.aethelsoft.grooveplayer.data.transfer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls pause/resume/cancel for active transfers.
 * The NearbyTransferOrchestrator checks these flags during chunk transfer.
 */
@Singleton
class TransferController @Inject constructor() {

    private val _pauseRequested = MutableStateFlow(false)
    val pauseRequested: StateFlow<Boolean> = _pauseRequested.asStateFlow()

    private val _cancelRequested = MutableStateFlow(false)
    val cancelRequested: StateFlow<Boolean> = _cancelRequested.asStateFlow()

    private var activeTransferId: Long? = null

    fun setActiveTransfer(transferId: Long?) {
        activeTransferId = transferId
        if (transferId == null) {
            _pauseRequested.value = false
            _cancelRequested.value = false
        }
    }

    fun requestPause(transferId: Long) {
        if (activeTransferId == transferId) {
            _pauseRequested.value = true
        }
    }

    fun requestResume(transferId: Long) {
        if (activeTransferId == transferId) {
            _pauseRequested.value = false
        }
    }

    fun requestCancel(transferId: Long) {
        if (activeTransferId == transferId) {
            _cancelRequested.value = true
        }
    }

    fun consumePauseRequested(): Boolean {
        val v = _pauseRequested.value
        if (v) _pauseRequested.value = false
        return v
    }

    fun consumeCancelRequested(): Boolean {
        val v = _cancelRequested.value
        if (v) {
            _cancelRequested.value = false
            activeTransferId = null
        }
        return v
    }

    fun isPauseRequested(): Boolean = _pauseRequested.value
    fun isCancelRequested(): Boolean = _cancelRequested.value
}
