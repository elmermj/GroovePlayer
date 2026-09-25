package com.aethelsoft.grooveplayer.data.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Upgrade prompt when cloud audio exists but the caller is not entitled to stream. */
@Singleton
class PremiumStreamSignals @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyRequired() {
        _events.tryEmit(Unit)
    }
}
