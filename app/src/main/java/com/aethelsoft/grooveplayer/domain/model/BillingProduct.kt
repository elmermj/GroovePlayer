package com.aethelsoft.grooveplayer.domain.model

enum class BillingProductKind {
    BASIC_SUB,
    PREMIUM_SUB,
    STORAGE_ADDON,
}

data class BillingProduct(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val kind: BillingProductKind,
    /** Play Billing subscription offer token required to launch purchase. */
    val offerToken: String? = null,
)
