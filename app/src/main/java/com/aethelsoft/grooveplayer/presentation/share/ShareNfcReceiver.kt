package com.aethelsoft.grooveplayer.presentation.share

import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Receives NFC session info when the Activity gets an NFC intent.
 * MainActivity forwards NFC intents here; ShareViaNfcScreen collects.
 */
object ShareNfcReceiver {
    private val _session = MutableSharedFlow<ShareSessionInfo>(replay = 0)
    val session: SharedFlow<ShareSessionInfo> = _session.asSharedFlow()

    suspend fun emit(info: ShareSessionInfo) {
        _session.emit(info)
    }

    fun tryEmit(info: ShareSessionInfo) = _session.tryEmit(info)
}
