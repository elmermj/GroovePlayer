package com.aethelsoft.grooveplayer

import android.app.Application
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GroovePlayerApp : Application() {

    @Inject
    lateinit var transferRepository: TransferRepository

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // A killed process can't finish a transfer: mark anything still
        // "active" in Room as FAILED so no ghost "Transferring" rows linger.
        appScope.launch {
            transferRepository.terminateActiveTransfers()
        }
    }
}
