package com.aethelsoft.grooveplayer.utils

/**
 * Canonical Google Play product IDs — see grooveplayer-backend/docs/play-billing.md
 * and app/BILLING_SETUP.md.
 *
 * Region PPP (T1/T2/T3) is Play country pricing on the same SKU; do not invent tier SKUs.
 */
object BillingProductIds {
    /** Basic monthly subscription — no ads ($0.99 US). */
    const val BASIC_MONTHLY = "groove_basic_monthly"

    /** Premium monthly subscription — 40 GB backup; regional price via Play. */
    const val PREMIUM_MONTHLY = "groove_premium_monthly"

    /** +20 GB stackable storage subscription; max 5 active. */
    const val STORAGE_20GB_MONTHLY = "groove_storage_20gb_monthly"

    val ALL: List<String> = listOf(
        BASIC_MONTHLY,
        PREMIUM_MONTHLY,
        STORAGE_20GB_MONTHLY,
    )

    fun isPremium(productId: String): Boolean = productId == PREMIUM_MONTHLY
    fun isBasic(productId: String): Boolean = productId == BASIC_MONTHLY
    fun isStorageAddon(productId: String): Boolean = productId == STORAGE_20GB_MONTHLY
}
