package com.aethelsoft.grooveplayer.domain.model

/**
 * Authenticated account identity returned by grooveplayer-backend (/v1/me, auth responses).
 */
data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val privilegeTier: PrivilegeTier,
    /**
     * From `/v1/me.storage`. May be [StorageEntitlement.isOptimisticStub] when Premium
     * but backend omits the object.
     */
    val storage: StorageEntitlement? = null,
)
