package com.aethelsoft.grooveplayer.domain.model

/**
 * One stackable +20 GB pack from `/v1/me.storage.addons[]`.
 * [expiresAtIso] always equals [StorageEntitlement.premiumPeriodEndIso] when Premium is active.
 * [cancelAtPeriodEnd] = stop renewal only; full quota kept until period end.
 */
data class StorageAddon(
    val id: String,
    val productId: String,
    val packGb: Int = 20,
    val status: String = "active",
    val cancelAtPeriodEnd: Boolean = false,
    val expiresAtIso: String? = null,
    val expiresAtEpochMs: Long? = null,
)
