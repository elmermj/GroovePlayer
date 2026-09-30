package com.aethelsoft.grooveplayer.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.wear.data.WatchRemoteBus
import com.aethelsoft.grooveplayer.wear.data.WatchSession
import com.aethelsoft.grooveplayer.wear.logic.RemoteUiState
import com.aethelsoft.grooveplayer.wear.logic.remoteUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    val artwork = WatchRemoteBus.artwork

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(250)
        }
    }

    val uiState: StateFlow<RemoteUiState> = combine(
        WatchRemoteBus.snapshot,
        WatchRemoteBus.link,
        ticker,
    ) { snapshot, link, now ->
        remoteUi(snapshot, link.connected, now)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        remoteUi(null, connected = false, nowMs = 0L),
    )

    init {
        WatchSession.ensureStarted(application)
    }

    fun playPause() = WatchSession.send(WearProtocol.PATH_PLAY_PAUSE)

    fun next() = WatchSession.send(WearProtocol.PATH_NEXT)

    fun previous() = WatchSession.send(WearProtocol.PATH_PREVIOUS)

    fun seekTo(positionMs: Long) {
        WatchSession.send(WearProtocol.PATH_SEEK, WearProtocol.encodeSeek(positionMs))
    }
}
