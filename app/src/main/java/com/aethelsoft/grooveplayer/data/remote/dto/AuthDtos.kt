package com.aethelsoft.grooveplayer.data.remote.dto

import com.squareup.moshi.Json

data class GoogleAuthRequestDto(
    @param:Json(name = "id_token") val idToken: String,
    @param:Json(name = "platform") val platform: String = "android",
)

data class RefreshRequestDto(
    @param:Json(name = "refresh_token") val refreshToken: String,
    @param:Json(name = "platform") val platform: String = "android",
)

data class LogoutRequestDto(
    @param:Json(name = "refresh_token") val refreshToken: String? = null,
    /** Default true — revoke every refresh for this user (docs/auth-logout.md). */
    @param:Json(name = "all_devices") val allDevices: Boolean? = true,
)


/** DELETE /v1/account — Benny contract (hard delete). Success is always 200 JSON, never 204. */
data class DeleteAccountResponseDto(
    @param:Json(name = "deleted") val deleted: Boolean = false,
    /** Idempotent: user already gone; still treat as success and sign out locally. */
    @param:Json(name = "already_gone") val alreadyGone: Boolean = false,
)

data class TokenResponseDto(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "refresh_token") val refreshToken: String,
    @param:Json(name = "token_type") val tokenType: String = "Bearer",
    @param:Json(name = "expires_in") val expiresIn: Long = 0L,
    @param:Json(name = "user") val user: PublicUserDto,
)

/**
 * Matches grooveplayer-backend PublicUser (/v1/me).
 * Renewal date + addons live under [storage], not top-level.
 */
data class PublicUserDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
    @param:Json(name = "tier") val tier: String = "free",
    @param:Json(name = "storage") val storage: StorageEntitlementDto? = null,
)

/** Matches backend models.AddonInfo */
data class StorageAddonDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "product_id") val productId: String,
    @param:Json(name = "pack_gb") val packGb: Int = 20,
    @param:Json(name = "status") val status: String = "active",
    /** Stop renewal only — entitlement kept until expires_at / premium_period_end. */
    @param:Json(name = "cancel_at_period_end") val cancelAtPeriodEnd: Boolean = false,
    /** Always aligns with storage.premium_period_end when Premium is active. */
    @param:Json(name = "expires_at") val expiresAt: String? = null,
)

/**
 * Matches backend StorageEntitlement JSON on /v1/me.storage
 * (docs/play-billing.md).
 */
data class StorageEntitlementDto(
    @param:Json(name = "quota_bytes") val quotaBytes: Long = 0L,
    @param:Json(name = "used_bytes") val usedBytes: Long = 0L,
    @param:Json(name = "addon_count") val addonCount: Int = 0,
    @param:Json(name = "usage_ratio") val usageRatio: Double? = null,
    @param:Json(name = "soft_warn") val softWarn: Boolean? = null,
    @param:Json(name = "hard_stop") val hardStop: Boolean? = null,
    @param:Json(name = "read_only") val readOnly: Boolean = false,
    /** Quota after period end (excludes cancel_at_period_end packs). */
    @param:Json(name = "pending_quota_bytes") val pendingQuotaBytes: Long? = null,
    /** True only after period-end drop (or other shrink) when used > current quota. */
    @param:Json(name = "over_quota") val overQuota: Boolean = false,
    @param:Json(name = "bytes_to_free") val bytesToFree: Long = 0L,
    @param:Json(name = "grace_until") val graceUntil: String? = null,
    /** Set at period end if over; +3d then auto latest trim (cloud only). */
    @param:Json(name = "grace_trim_until") val graceTrimUntil: String? = null,
    /** Single shared renewal date for Premium + all +20GB packs (Jorge). */
    @param:Json(name = "premium_period_end") val premiumPeriodEnd: String? = null,
    @param:Json(name = "addons") val addons: List<StorageAddonDto> = emptyList(),
    @param:Json(name = "suggested_strategies") val suggestedStrategies: List<String> = emptyList(),
    @param:Json(name = "max_addon_packs") val maxAddonPacks: Int = 5,
)

/** POST /v1/billing/play/verify — see docs/play-billing.md */
data class VerifyPurchaseRequestDto(
    @param:Json(name = "product_id") val productId: String,
    @param:Json(name = "purchase_token") val purchaseToken: String,
    @param:Json(name = "package_name") val packageName: String? = "com.aethelsoft.grooveplayer",
)

/** POST /v1/billing/play/ack */
data class AckPurchaseRequestDto(
    @param:Json(name = "product_id") val productId: String,
    @param:Json(name = "purchase_token") val purchaseToken: String,
)


/** POST /v1/billing/addons/cancel — docs/addon-cancel-trim.md */
data class CancelAddonRequestDto(
    @param:Json(name = "addon_id") val addonId: String,
)

data class CancelAddonResponseDto(
    @param:Json(name = "cancelled_addon_id") val cancelledAddonId: String? = null,
    @param:Json(name = "cancel_at_period_end") val cancelAtPeriodEnd: Boolean = true,
    @param:Json(name = "addon_count") val addonCount: Int? = null,
    @param:Json(name = "pending_quota_bytes") val pendingQuotaBytes: Long? = null,
    @param:Json(name = "user") val user: PublicUserDto? = null,
    @param:Json(name = "note") val note: String? = null,
)

data class ApiErrorDto(
    @param:Json(name = "error") val error: String? = null,
)
