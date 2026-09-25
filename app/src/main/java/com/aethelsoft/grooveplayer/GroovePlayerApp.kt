package com.aethelsoft.grooveplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.aethelsoft.grooveplayer.data.artwork.EmbeddedArtworkFetcher
import com.aethelsoft.grooveplayer.data.artwork.EmbeddedArtworkKeyer
import com.aethelsoft.grooveplayer.data.artwork.SongArtworkFiles
import com.aethelsoft.grooveplayer.domain.artwork.EmbeddedArtworkCache
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelsoft.grooveplayer.domain.usecase.ads_category.PublishAdEntitlementUseCase
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GroovePlayerApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var songArtworkFiles: SongArtworkFiles

    @Inject
    lateinit var transferRepository: TransferRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var publishAdEntitlement: PublishAdEntitlementUseCase

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cache = EmbeddedArtworkCache(File(cacheDir, "embedded-artwork"))
        return ImageLoader.Builder(context)
            .components {
                add(EmbeddedArtworkKeyer())
                add(EmbeddedArtworkFetcher.Factory(songArtworkFiles, cache))
            }
            .build()
    }

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
