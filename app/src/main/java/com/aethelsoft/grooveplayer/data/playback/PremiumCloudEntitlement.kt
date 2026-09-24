package com.aethelsoft.grooveplayer.data.playback

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamEntitlement
import com.aethelsoft.grooveplayer.domain.playback.activeCloudStreamEntitled
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active-premium gate for streaming, matching the server: tier premium and
 * grace closed. Free, basic, and an open `grace_until` are not entitled.
 * This check is not a purge signal. Purge runs only on 404 `{exists:false}`.
 *
 * Availability badges stay on tier premium ([isPremiumNow]), including grace,
 * so a premium row can still show a cloud mark while playback offers upgrade.
 */
@Singleton
class PremiumCloudEntitlement @Inject constructor(
    private val authRepository: AuthRepository,
) : CloudStreamEntitlement {

    val showAvailability: Flow<Boolean> =
        authRepository.observePrivilegeTier().map { it == PrivilegeTier.PREMIUM }

    fun isPremiumNow(): Boolean = authRepository.currentPrivilegeTier() == PrivilegeTier.PREMIUM

    override suspend fun canStreamFromCloud(): Boolean {
        val user = authRepository.getAuthUser() ?: return false
        val storage = user.storage
        val graceUntil = if (storage?.isOptimisticStub == true) null else storage?.graceUntilEpochMs
        return activeCloudStreamEntitled(
            tier = user.privilegeTier,
            graceUntilEpochMs = graceUntil,
            nowEpochMs = System.currentTimeMillis(),
        )
    }
}
