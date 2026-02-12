package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.data.share.ShareTransferState
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import com.aethelsoft.grooveplayer.domain.model.ShareableItem
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ShareRepository {

    val transferState: StateFlow<ShareTransferState>

    /**
     * Start as sender: open server, advertise via sessionInfo.
     * Caller must pass sessionInfo from NFC or NSD.
     */
    suspend fun startSender(items: List<Song>, sessionInfo: ShareSessionInfo)

    /**
     * Start as receiver: connect to sessionInfo, receive offer, return approved ids.
     */
    suspend fun connectAndReceiveOffer(sessionInfo: ShareSessionInfo): List<ShareableItem>?

    /**
     * Receiver approves items and starts receiving.
     */
    suspend fun approveAndReceive(approvedIds: List<String>)

    /**
     * Receiver rejects the offer.
     */
    suspend fun rejectOffer()

    fun cancelTransfer()
}
