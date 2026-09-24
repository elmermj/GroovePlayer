package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Ads are shown ONLY for FREE tier (including signed-out).
 * BASIC and PREMIUM remove ads. No Premium upsell UI.
 */
class ShouldShowAdsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        authRepository.observePrivilegeTier().map { it == PrivilegeTier.FREE }

    fun forTier(tier: PrivilegeTier): Boolean = tier == PrivilegeTier.FREE
}
