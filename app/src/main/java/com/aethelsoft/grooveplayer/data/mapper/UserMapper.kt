package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.local.db.entity.UserProfileEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.UserSettingsEntity
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.model.UserSettings

/**
 * Mapper for converting between data layer and domain layer user_category models.
 * Follows Clean Architecture by keeping layers separated.
 */
object UserMapper {
    
    // UserProfile mappings
    fun userProfileToDomain(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            username = entity.username,
            email = entity.email,
            profilePictureUrl = entity.profilePictureUrl,
            privilegeTier = entity.privilegeTier,
            settingsReferences = parseSettingsReferences(entity.settingsReferences),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    fun userProfileToEntity(domain: UserProfile): UserProfileEntity {
        return UserProfileEntity(
            id = domain.id,
            username = domain.username,
            email = domain.email,
            profilePictureUrl = domain.profilePictureUrl,
            privilegeTier = domain.privilegeTier,
            settingsReferences = settingsReferencesToString(domain.settingsReferences),
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    // UserSettings mappings
    fun userSettingsToDomain(entity: UserSettingsEntity): UserSettings {
        return UserSettings(
            id = entity.id,
            lastPlayedSongsTimer = entity.lastPlayedSongsTimer,
            fadeTimer = entity.fadeTimer
        )
    }
    
    fun userSettingsToEntity(domain: UserSettings): UserSettingsEntity {
        return UserSettingsEntity(
            id = domain.id,
            lastPlayedSongsTimer = domain.lastPlayedSongsTimer,
            fadeTimer = domain.fadeTimer
        )
    }
    
    // Helper functions for List<Int> <-> String conversion
    private fun parseSettingsReferences(value: String): List<Int> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
    
    private fun settingsReferencesToString(list: List<Int>): String {
        return list.joinToString(",")
    }
}
