package com.aethelsoft.grooveplayer.domain.model

/**
 * Domain model representing a user_category's profile.
 * Contains user_category identity and subscription information.
 */
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val privilegeTier: PrivilegeTier = PrivilegeTier.FREE,
    val settingsReferences: List<Int> = emptyList(),
    val createdAt: Long,  // Timestamp
    val updatedAt: Long   // Timestamp
)
