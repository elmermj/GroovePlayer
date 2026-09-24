package com.aethelsoft.grooveplayer.domain.model

/**
 * Cloud backup quota from `/v1/me.storage` (Benny).
 *
 * Cancel mid-period does **not** drop [quotaBytes]; see [pendingQuotaBytes] for after-period UI.
 * [overQuota] / [bytesToFree] / [graceTrimUntilIso] appear only after period-end drop (or other shrink).
 *
 * When the backend omits `storage`, the client may synthesize an
 * [isOptimisticStub] entitlement for Premium UI — clearly marked, not authoritative.
 */
data class StorageEntitlement(
    val quotaBytes: Long,
    val usedBytes: Long,
    val addonCount: Int = 0,
    val usageRatio: Double = if (quotaBytes > 0) usedBytes.toDouble() / quotaBytes else 0.0,
    val softWarn: Boolean = quotaBytes > 0 && usageRatio >= 0.80 && usedBytes < quotaBytes,
    val hardStop: Boolean = quotaBytes > 0 && usedBytes >= quotaBytes,
    /** True during Premium cancel grace — or while [overQuota] (uploads blocked). */
    val readOnly: Boolean = false,
    /**
     * Quota after period end (excludes packs with [StorageAddon.cancelAtPeriodEnd]).
     * Equal to [quotaBytes] when nothing is pending cancel.
     */
    val pendingQuotaBytes: Long? = null,
    /** used > **current** quota — only after period-end drop / shrink. */
    val overQuota: Boolean = false,
    val bytesToFree: Long = 0L,
    val graceUntilIso: String? = null,
    val graceUntilEpochMs: Long? = null,
    /** Over-quota trim grace (+3d then auto `latest` on cloud). */
    val graceTrimUntilIso: String? = null,
    val graceTrimUntilEpochMs: Long? = null,
    /** Single renewal date for Premium + all add-ons (`storage.premium_period_end`). */
    val premiumPeriodEndIso: String? = null,
    val premiumPeriodEndEpochMs: Long? = null,
    val addons: List<StorageAddon> = emptyList(),
    val suggestedStrategies: List<String> = emptyList(),
    val maxAddonPacks: Int = 5,
    /**
     * OPTIMISTIC_STUB: local placeholder while `/v1/me` lacks `storage`.
     * Do not treat as server truth for wipe/grace decisions.
     */
    val isOptimisticStub: Boolean = false,
) {
    companion object {
        const val PREMIUM_BASE_QUOTA_BYTES: Long = 40L * 1024 * 1024 * 1024 // 40 GB
        const val ADDON_PACK_BYTES: Long = 20L * 1024 * 1024 * 1024 // 20 GB
        const val MAX_ADDON_PACKS: Int = 5
        const val WARN_RATIO: Double = 0.80

        val DEFAULT_TRIM_STRATEGIES: List<String> = listOf(
            "latest", "oldest", "smallest", "largest",
            "by_artist", "by_album", "by_year", "random_fill",
        )

        fun optimisticPremiumStub(
            usedBytes: Long = 0L,
            addonCount: Int = 0,
        ): StorageEntitlement {
            val safeAddons = addonCount.coerceIn(0, MAX_ADDON_PACKS)
            val quota = PREMIUM_BASE_QUOTA_BYTES + safeAddons * ADDON_PACK_BYTES
            return StorageEntitlement(
                quotaBytes = quota,
                usedBytes = usedBytes,
                addonCount = safeAddons,
                pendingQuotaBytes = quota,
                isOptimisticStub = true,
            )
        }

        fun none(): StorageEntitlement = StorageEntitlement(
            quotaBytes = 0L,
            usedBytes = 0L,
            hardStop = true,
        )
    }
}
