package com.aethelsoft.grooveplayer.data.share

import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo

sealed class ShareTransferState {
    data object Idle : ShareTransferState()
    data object Connecting : ShareTransferState()
    data class Offering(val items: List<ShareableItem>) : ShareTransferState()
    data object WaitingApproval : ShareTransferState()
    data class Transferring(
        val currentItem: ShareableItem,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val itemIndex: Int,
        val totalItems: Int
    ) : ShareTransferState()
    data object Done : ShareTransferState()
    data class Error(val message: String) : ShareTransferState()
}

sealed class ShareRole {
    data object Sender : ShareRole()
    data object Receiver : ShareRole()
}
