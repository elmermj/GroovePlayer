package com.aethelsoft.grooveplayer.data.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareTransferManager @Inject constructor() {

    private val _state = MutableStateFlow<ShareTransferState>(ShareTransferState.Idle)
    val state: StateFlow<ShareTransferState> = _state.asStateFlow()

    fun setState(s: ShareTransferState) {
        _state.value = s
    }

    fun setConnecting() {
        _state.value = ShareTransferState.Connecting
    }

    fun setOffering(items: List<com.aethelsoft.grooveplayer.domain.model.ShareableItem>) {
        _state.value = ShareTransferState.Offering(items)
    }

    fun setWaitingApproval() {
        _state.value = ShareTransferState.WaitingApproval
    }

    fun setTransferring(
        currentItem: com.aethelsoft.grooveplayer.domain.model.ShareableItem,
        bytesTransferred: Long,
        totalBytes: Long,
        itemIndex: Int,
        totalItems: Int
    ) {
        _state.value = ShareTransferState.Transferring(
            currentItem = currentItem,
            bytesTransferred = bytesTransferred,
            totalBytes = totalBytes,
            itemIndex = itemIndex,
            totalItems = totalItems
        )
    }

    fun setDone() {
        _state.value = ShareTransferState.Done
    }

    fun setError(message: String) {
        _state.value = ShareTransferState.Error(message)
    }

    fun setIdle() {
        _state.value = ShareTransferState.Idle
    }
}
