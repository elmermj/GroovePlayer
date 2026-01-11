package com.aethelsoft.grooveplayer.domain.model

/**
 * User privilege tier for app features and subscription management.
 * Domain model - framework independent.
 */
enum class PrivilegeTier {
    FREE,      // Free tier with ads
    BASIC,     // Basic tier with limited features
    PREMIUM    // Premium tier with all features
}
