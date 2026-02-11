package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.local.db.entity.UserProfileEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.UserSettingsEntity
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.model.UserSettings
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode

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
            fadeTimer = entity.fadeTimer,
            equalizerEnabled = entity.equalizerEnabled,
            equalizerPreset = entity.equalizerPreset,
            equalizerBandLevels = parseBandLevels(entity.equalizerBandLevels),
            lastPlayedSongId = entity.lastPlayedSongId,
            lastPlayedPosition = entity.lastPlayedPosition,
            shuffleEnabled = entity.shuffleEnabled,
            repeatMode = entity.repeatMode,
            queueSongIds = parseSongIds(entity.queueSongIds),
            queueStartIndex = entity.queueStartIndex,
            isEndlessQueue = entity.isEndlessQueue,
            visualizationMode = when (entity.visualizationMode) {
                "OFF" -> VisualizationMode.OFF
                "SIMULATED" -> VisualizationMode.SIMULATED
                "REAL_TIME" -> VisualizationMode.REAL_TIME
                else -> VisualizationMode.REAL_TIME
            },
            showMiniPlayerOnStart = entity.showMiniPlayerOnStart,
            excludedFolders = parseExcludedFolders(entity.excludedFolders)
        )
    }
    
    fun userSettingsToEntity(domain: UserSettings): UserSettingsEntity {
        return UserSettingsEntity(
            id = domain.id,
            lastPlayedSongsTimer = domain.lastPlayedSongsTimer,
            fadeTimer = domain.fadeTimer,
            equalizerEnabled = domain.equalizerEnabled,
            equalizerPreset = domain.equalizerPreset,
            equalizerBandLevels = bandLevelsToString(domain.equalizerBandLevels),
            lastPlayedSongId = domain.lastPlayedSongId,
            lastPlayedPosition = domain.lastPlayedPosition,
            shuffleEnabled = domain.shuffleEnabled,
            repeatMode = domain.repeatMode,
            queueSongIds = songIdsToString(domain.queueSongIds),
            queueStartIndex = domain.queueStartIndex,
            isEndlessQueue = domain.isEndlessQueue,
            visualizationMode = when (domain.visualizationMode) {
                VisualizationMode.OFF -> "OFF"
                VisualizationMode.SIMULATED -> "SIMULATED"
                VisualizationMode.REAL_TIME -> "REAL_TIME"
            },
            showMiniPlayerOnStart = domain.showMiniPlayerOnStart,
            excludedFolders = excludedFoldersToString(domain.excludedFolders)
        )
    }
    
    // Helper functions for excluded folder paths (delimiter "||" so paths with commas are safe)
    private fun parseExcludedFolders(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split("||").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    
    private fun excludedFoldersToString(list: List<String>): String {
        return list.joinToString("||")
    }
    
    // Helper functions for List<Int> <-> String conversion for band levels
    private fun parseBandLevels(value: String): List<Int> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
    
    private fun bandLevelsToString(list: List<Int>): String {
        return list.joinToString(",")
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
    
    // Helper functions for List<String> <-> String conversion for song IDs
    private fun parseSongIds(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    
    private fun songIdsToString(list: List<String>): String {
        return list.joinToString(",")
    }
}
