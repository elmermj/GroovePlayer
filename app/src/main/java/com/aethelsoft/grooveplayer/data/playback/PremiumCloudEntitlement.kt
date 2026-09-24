package com.aethelsoft.grooveplayer.data.playback

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.playback.CloudStreamEntitlement
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud streaming and availability badges share this gate: Premium only.
 * Free and Basic never stream and never see a badge.
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
