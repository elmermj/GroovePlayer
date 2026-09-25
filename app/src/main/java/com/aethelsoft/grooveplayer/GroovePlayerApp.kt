package com.aethelsoft.grooveplayer

import android.app.Application
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelsoft.grooveplayer.domain.usecase.ads_category.PublishAdEntitlementUseCase
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

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var publishAdEntitlement: PublishAdEntitlementUseCase

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // MobileAds.initialize runs after UMP consent in MainActivity (AdsConsentHelper).
        appScope.launch {
            transferRepository.terminateActiveTransfers()
        }
        appScope.launch {
            try {
                authRepository.restoreSession()
            } finally {
                // Opens the ad gate only once the tier is known. Unknown stays closed.
                publishAdEntitlement()
            }
        }
    }
}
