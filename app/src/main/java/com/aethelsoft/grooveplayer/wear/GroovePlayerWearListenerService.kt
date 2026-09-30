package com.aethelsoft.grooveplayer.wear

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GroovePlayerWearListenerService : WearableListenerService() {
    @Inject lateinit var executor: WearCommandExecutor
    @Inject lateinit var publisher: WearStatePublisher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        publisher.start()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val command = WearProtocol.parseCommand(messageEvent.path, messageEvent.data) ?: return
        publisher.noteWatchReachable()
        scope.launch {
            runCatching { executor.execute(command) }
                .onFailure { error -> Log.w(TAG, "Wear command failed", error) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "GrooveWearListener"
    }
}
