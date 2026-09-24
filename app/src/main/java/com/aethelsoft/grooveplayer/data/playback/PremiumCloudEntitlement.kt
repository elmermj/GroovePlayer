package com.aethelsoft.grooveplayer.data.playback

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamEntitlement
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium gate for streaming an object that already exists, and for availability badges.
 * Free and Basic are not entitled: playback skips and the catalog row stays.
 * This check is not a purge signal. Purge runs only when the object is confirmed absent.
 */
@Singleton
class PremiumCloudEntitlement @Inject constructor(
    private val authRepository: AuthRepository,
) : CloudStreamEntitlement {

    val showAvailability: Flow<Boolean> =
        authRepository.observePrivilegeTier().map { it == PrivilegeTier.PREMIUM }

    fun isPremiumNow(): Boolean = authRepository.currentPrivilegeTier() == PrivilegeTier.PREMIUM

    override suspend fun canStreamFromCloud(): Boolean = isPremiumNow()
}
