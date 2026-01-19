package com.aethelsoft.grooveplayer.domain.model

/**
 * User privilege tier for app features and subscription management.
 * Domain model - framework independent.
 */
enum class PrivilegeTier {
    FREE,      // Free tier with ads and users who are not logged in
    BASIC,     // Basic tier with limited features or users who are logged in
    PREMIUM    // Premium tier with all features
}
