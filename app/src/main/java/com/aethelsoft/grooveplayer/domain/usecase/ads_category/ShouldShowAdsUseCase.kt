package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Ads only for a confirmed FREE tier (including signed-out, after session restore).
 * BASIC and PREMIUM remove ads. Unknown or still loading shows nothing.
 */
class ShouldShowAdsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val readiness: AdEntitlementReadiness,
) {
    /**
     * Null while the privilege tier is unknown or still loading.
     * False for Basic/Premium. True only for confirmed Free.
     */
    fun observeDecision(): Flow<Boolean?> =
        combine(
            readiness.observe(),
            authRepository.observePrivilegeTier(),
        ) { resolved, tier ->
            if (!resolved) null else AdGate.shouldShowAds(entitlementResolved = true, tier)
        }

    operator fun invoke(): Flow<Boolean> = observeDecision().map { it == true }

    fun canShowNow(): Boolean =
        AdGate.shouldShowAds(readiness.isResolved(), authRepository.currentPrivilegeTier())

    /** Tier check once entitlement is already confirmed. */
    fun forTier(tier: PrivilegeTier): Boolean =
        AdGate.shouldShowAds(entitlementResolved = true, tier)
}
