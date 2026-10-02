package com.aethelworks.grooveplayer.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.wear.data.WatchRemoteBus
import com.aethelworks.grooveplayer.wear.data.WatchSession
import com.aethelworks.grooveplayer.wear.logic.RemoteStatusCopy
import com.aethelworks.grooveplayer.wear.logic.RemoteUiState
import com.aethelworks.grooveplayer.wear.logic.remoteUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    val artwork = WatchRemoteBus.artwork

    private val statusCopy = RemoteStatusCopy(
        disconnectedTitle = application.getString(R.string.wear_disconnected),
        disconnectedArtist = application.getString(R.string.wear_open_phone),
        waitingTitle = application.getString(R.string.wear_waiting_title),
        waitingArtist = application.getString(R.string.wear_waiting_artist),
        nothingPlaying = application.getString(R.string.wear_nothing_playing),
    )

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
        remoteUi(snapshot, link.connected, now, statusCopy)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        remoteUi(null, connected = false, nowMs = 0L, copy = statusCopy),
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
