package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier

/**
 * Ads are allowed only after the privilege tier is confirmed Free.
 * Loading, unknown, Basic, and Premium show nothing — including the process
 * restart after a library restore, before `/v1/me` has filled the in-memory user.
 *
 * A missing user is not Free. The auth repository maps that gap to [PrivilegeTier.FREE],
 * so callers must pass [entitlementResolved] from [AdEntitlementReadiness] rather than
 * trusting the tier alone.
 */
object AdGate {
    fun shouldShowAds(entitlementResolved: Boolean, tier: PrivilegeTier): Boolean =
        entitlementResolved && tier == PrivilegeTier.FREE

    /**
     * Session restore has an answer.
     * A missing user is confirmed signed-out Free only when no session token exists.
     * Tokens without a user mean the tier is still unknown (fail closed: no ads).
     */
    fun isEntitlementConfirmed(userPresent: Boolean, hasSessionToken: Boolean): Boolean =
        userPresent || !hasSessionToken
}
