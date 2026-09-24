package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.remote.dto.PublicUserDto
import com.aethelsoft.grooveplayer.data.remote.dto.StorageAddonDto
import com.aethelsoft.grooveplayer.data.remote.dto.StorageEntitlementDto
import com.aethelsoft.grooveplayer.data.remote.dto.TokenResponseDto
import com.aethelsoft.grooveplayer.domain.model.AuthTokens
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.StorageAddon
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import java.time.Instant
import java.time.format.DateTimeParseException

object AuthMapper {
    fun tierFromServer(raw: String?): PrivilegeTier = when (raw?.lowercase()) {
        "basic" -> PrivilegeTier.BASIC
        "premium" -> PrivilegeTier.PREMIUM
        else -> PrivilegeTier.FREE
    }

    fun toDomainUser(dto: PublicUserDto): AuthUser {
        val tier = tierFromServer(dto.tier)
        // /v1/me.storage is live (null/[] fields for free). Map as-is — no field stubs.
        val storage = dto.storage?.let { toStorage(it) }
        return AuthUser(
            id = dto.id,
            email = dto.email,
            displayName = dto.displayName,
            avatarUrl = dto.avatarUrl,
            privilegeTier = tier,
            storage = storage,
        )
    }

    fun toStorage(dto: StorageEntitlementDto): StorageEntitlement {
        val ratio = dto.usageRatio
            ?: if (dto.quotaBytes > 0) dto.usedBytes.toDouble() / dto.quotaBytes else 0.0
        val over = dto.overQuota ||
            (dto.quotaBytes >= 0 && dto.usedBytes > dto.quotaBytes && dto.quotaBytes > 0)
        val soft = dto.softWarn
            ?: (dto.quotaBytes > 0 && ratio >= StorageEntitlement.WARN_RATIO && dto.usedBytes < dto.quotaBytes)
        val hard = (dto.hardStop
            ?: (dto.quotaBytes > 0 && dto.usedBytes >= dto.quotaBytes)) || over
        val periodIso = dto.premiumPeriodEnd
        val pending = dto.pendingQuotaBytes
        val bytesToFree = when {
            dto.bytesToFree > 0L -> dto.bytesToFree
            over && dto.quotaBytes >= 0 -> (dto.usedBytes - dto.quotaBytes).coerceAtLeast(0L)
            else -> 0L
        }
        val strategies = dto.suggestedStrategies.ifEmpty {
            StorageEntitlement.DEFAULT_TRIM_STRATEGIES
        }
        return StorageEntitlement(
            quotaBytes = dto.quotaBytes,
            usedBytes = dto.usedBytes,
            addonCount = dto.addonCount,
            usageRatio = ratio,
            softWarn = soft,
            hardStop = hard,
            // Backend sets read_only during premium grace OR while over_quota.
            readOnly = dto.readOnly || over,
            pendingQuotaBytes = pending,
            overQuota = over,
            bytesToFree = bytesToFree,
            graceUntilIso = dto.graceUntil,
            graceUntilEpochMs = parseIsoEpochMs(dto.graceUntil),
            graceTrimUntilIso = dto.graceTrimUntil,
            graceTrimUntilEpochMs = parseIsoEpochMs(dto.graceTrimUntil),
            premiumPeriodEndIso = periodIso,
            premiumPeriodEndEpochMs = parseIsoEpochMs(periodIso),
            addons = dto.addons.map { toAddon(it) },
            suggestedStrategies = strategies,
            maxAddonPacks = dto.maxAddonPacks,
            isOptimisticStub = false,
        )
    }

    fun toAddon(dto: StorageAddonDto): StorageAddon = StorageAddon(
        id = dto.id,
        productId = dto.productId,
        packGb = dto.packGb,
        status = dto.status,
        cancelAtPeriodEnd = dto.cancelAtPeriodEnd,
        expiresAtIso = dto.expiresAt,
        expiresAtEpochMs = parseIsoEpochMs(dto.expiresAt),
    )

    fun parseIsoEpochMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        raw.toLongOrNull()?.let { return it }
        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(
                    raw.replace(" ", "T") +
                        if (!raw.endsWith("Z") && '+' !in raw && raw.count { it == '-' } < 3) "Z" else "",
                ).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toTokens(dto: TokenResponseDto): AuthTokens = AuthTokens(
        accessToken = dto.accessToken,
        refreshToken = dto.refreshToken,
        tokenType = dto.tokenType,
        expiresInSeconds = dto.expiresIn,
    )

    fun toUserProfile(user: AuthUser, now: Long = System.currentTimeMillis()): UserProfile =
        UserProfile(
            id = user.id,
            username = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "User",
            email = user.email.orEmpty(),
            profilePictureUrl = user.avatarUrl,
            privilegeTier = user.privilegeTier,
            settingsReferences = emptyList(),
            createdAt = now,
            updatedAt = now,
        )
}
