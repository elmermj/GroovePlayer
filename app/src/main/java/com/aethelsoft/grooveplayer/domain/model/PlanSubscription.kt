package com.aethelsoft.grooveplayer.domain.model

/**
 * Top-level `subscription` object on GET /v1/me.
 * Null when the server omits it (free users and older servers).
 */
data class PlanSubscription(
    val planKind: PrivilegeTier,
    val status: String,
    val currentPeriodEndEpochMs: Long?,
    val autoRenew: Boolean,
    val graceUntilEpochMs: Long?,
)
