package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.UserProfileDao
import com.aethelsoft.grooveplayer.data.local.db.dao.UserSettingsDao
import com.aethelsoft.grooveplayer.data.local.db.entity.UserSettingsEntity
import com.aethelsoft.grooveplayer.data.mapper.UserMapper
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.model.UserSettings
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository.
 * Handles user_category profile and settings operations using Room database.
 * Maps between data layer entities and domain models.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val userSettingsDao: UserSettingsDao
) : UserRepository {
    
    // ============ UserProfile Operations ============
    
    override fun observeUserProfile(): Flow<UserProfile?> {
        return userProfileDao.observeUserProfile().map { entity ->
            entity?.let { UserMapper.userProfileToDomain(it) }
        }
    }
    
    override suspend fun getUserProfile(): UserProfile? {
        return userProfileDao.getUserProfile()?.let { entity ->
            UserMapper.userProfileToDomain(entity)
        }
    }
    
    override suspend fun saveUserProfile(profile: UserProfile) {
        val entity = UserMapper.userProfileToEntity(profile)
        userProfileDao.insertUserProfile(entity)
    }
    
    override suspend fun updateUsername(username: String) {
        val profile = userProfileDao.getUserProfile()
        if (profile != null) {
            userProfileDao.updateUsername(
                userId = profile.id,
                username = username,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun updateEmail(email: String) {
        val profile = userProfileDao.getUserProfile()
        if (profile != null) {
            userProfileDao.updateEmail(
                userId = profile.id,
                email = email,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun updateProfilePicture(url: String?) {
        val profile = userProfileDao.getUserProfile()
        if (profile != null) {
            userProfileDao.updateProfilePicture(
                userId = profile.id,
                url = url,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun updatePrivilegeTier(tier: PrivilegeTier) {
        val profile = userProfileDao.getUserProfile()
        if (profile != null) {
            userProfileDao.updatePrivilegeTier(
                userId = profile.id,
                tier = tier,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun deleteUserProfile() {
        userProfileDao.deleteUserProfile()
    }
    
    // ============ UserSettings Operations ============
    
    override fun observeUserSettings(): Flow<UserSettings> {
        return userSettingsDao.observeUserSettings()
            .map { entity ->
                entity?.let { UserMapper.userSettingsToDomain(it) }
                    ?: getDefaultSettings() // Return defaults if no settings exist
            }
            .catch { e ->
                android.util.Log.e("UserRepository", "Error observing settings", e)
                emit(getDefaultSettings())
            }
    }
    
    override suspend fun getUserSettings(): UserSettings {
        return try {
            userSettingsDao.getUserSettings()?.let { entity ->
                UserMapper.userSettingsToDomain(entity)
            } ?: run {
                // If no settings exist, create and return defaults
                val defaults = getDefaultSettings()
                saveUserSettings(defaults)
                defaults
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to get user settings, returning defaults", e)
            getDefaultSettings()
        }
    }
    
    override suspend fun saveUserSettings(settings: UserSettings) {
        val entity = UserMapper.userSettingsToEntity(settings)
        userSettingsDao.insertUserSettings(entity)
    }
    
    override suspend fun updateLastPlayedSongsTimer(days: Int) {
        // Ensure settings exist before updating
        if (userSettingsDao.getUserSettings() == null) {
            userSettingsDao.insertUserSettings(UserSettingsEntity(id = 1))
        }
        userSettingsDao.updateLastPlayedSongsTimer(days)
    }
    
    override suspend fun updateFadeTimer(seconds: Int) {
        // Ensure settings exist before updating
        if (userSettingsDao.getUserSettings() == null) {
            userSettingsDao.insertUserSettings(UserSettingsEntity(id = 1))
        }
        userSettingsDao.updateFadeTimer(seconds)
    }
    
    override suspend fun updateRepeatAndShuffle(shuffle: Boolean, repeat: String) {
        if (userSettingsDao.getUserSettings() == null) {
            userSettingsDao.insertUserSettings(
                UserSettingsEntity(id = 1, shuffleEnabled = shuffle, repeatMode = repeat)
            )
        } else {
            userSettingsDao.updateRepeatAndShuffle(shuffle, repeat)
        }
    }

    override suspend fun updateShowMiniPlayerOnStart(enabled: Boolean) {
        if (userSettingsDao.getUserSettings() == null) {
            userSettingsDao.insertUserSettings(
                UserSettingsEntity(id = 1, showMiniPlayerOnStart = enabled)
            )
        } else {
            userSettingsDao.updateShowMiniPlayerOnStart(enabled)
        }
    }

    override suspend fun updateVisualizationMode(mode: com.aethelsoft.grooveplayer.domain.model.VisualizationMode) {
        // Ensure settings exist before updating
        if (userSettingsDao.getUserSettings() == null) {
            userSettingsDao.insertUserSettings(
                UserSettingsEntity(
                    id = 1,
                    lastPlayedSongsTimer = 30,
                    fadeTimer = 0,
                    visualizationMode = when (mode) {
                        com.aethelsoft.grooveplayer.domain.model.VisualizationMode.OFF -> "OFF"
                        com.aethelsoft.grooveplayer.domain.model.VisualizationMode.SIMULATED -> "SIMULATED"
                        com.aethelsoft.grooveplayer.domain.model.VisualizationMode.REAL_TIME -> "REAL_TIME"
                    }
                )
            )
        } else {
            userSettingsDao.updateVisualizationMode(
                when (mode) {
                    com.aethelsoft.grooveplayer.domain.model.VisualizationMode.OFF -> "OFF"
                    com.aethelsoft.grooveplayer.domain.model.VisualizationMode.SIMULATED -> "SIMULATED"
                    com.aethelsoft.grooveplayer.domain.model.VisualizationMode.REAL_TIME -> "REAL_TIME"
                }
            )
        }
    }
    
    override suspend fun updateEqualizerSettings(
        enabled: Boolean,
        preset: Int,
        bandLevels: List<Int>
    ) {
        try {
            // Ensure settings exist before updating
            if (userSettingsDao.getUserSettings() == null) {
                userSettingsDao.insertUserSettings(
                    UserSettingsEntity(
                        id = 1,
                        lastPlayedSongsTimer = 30,
                        fadeTimer = 0,
                        equalizerEnabled = enabled,
                        equalizerPreset = preset,
                        equalizerBandLevels = bandLevels.joinToString(",")
                    )
                )
            } else {
                val bandLevelsString = bandLevels.joinToString(",")
                userSettingsDao.updateEqualizerSettings(enabled, preset, bandLevelsString)
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepositoryImpl", "Error updating equalizer settings: ${e.message}", e)
            throw e
        }
    }
    
    override suspend fun updateLastPlayedSong(songId: String?, position: Long) {
        try {
            // Ensure settings exist before updating
            if (userSettingsDao.getUserSettings() == null) {
                userSettingsDao.insertUserSettings(
                    UserSettingsEntity(
                        id = 1,
                        lastPlayedSongsTimer = 30,
                        fadeTimer = 0,
                        lastPlayedSongId = songId,
                        lastPlayedPosition = position
                    )
                )
            } else {
                userSettingsDao.updateLastPlayedSong(songId, position)
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepositoryImpl", "Error updating last played song: ${e.message}", e)
        }
    }
    
    override suspend fun updatePlayerState(
        songId: String?,
        position: Long,
        shuffle: Boolean,
        repeat: String,
        queueSongIds: List<String>,
        queueStartIndex: Int,
        isEndlessQueue: Boolean
    ) {
        try {
            // Ensure settings exist before updating
            if (userSettingsDao.getUserSettings() == null) {
                userSettingsDao.insertUserSettings(
                    UserSettingsEntity(
                        id = 1,
                        lastPlayedSongsTimer = 30,
                        fadeTimer = 0,
                        lastPlayedSongId = songId,
                        lastPlayedPosition = position,
                        shuffleEnabled = shuffle,
                        repeatMode = repeat,
                        queueSongIds = queueSongIds.joinToString(","),
                        queueStartIndex = queueStartIndex,
                        isEndlessQueue = isEndlessQueue
                    )
                )
            } else {
                userSettingsDao.updatePlayerState(
                    songId = songId,
                    position = position,
                    shuffle = shuffle,
                    repeat = repeat,
                    queueSongIds = queueSongIds.joinToString(","),
                    queueStartIndex = queueStartIndex,
                    isEndlessQueue = isEndlessQueue
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepositoryImpl", "Error updating player state: ${e.message}", e)
        }
    }
    
    override suspend fun resetSettingsToDefault() {
        userSettingsDao.resetToDefault()
    }
    
    // ============ Private Helper Methods ============
    
    private fun getDefaultSettings(): UserSettings {
        return UserSettings(
            id = 1,
            lastPlayedSongsTimer = 30,  // 30 days
            fadeTimer = 0,              // No fade
            equalizerEnabled = false,
            equalizerPreset = -1,
            equalizerBandLevels = emptyList(),
            lastPlayedSongId = null,
            lastPlayedPosition = 0L,
            shuffleEnabled = false,
            repeatMode = "OFF",
            queueSongIds = emptyList(),
            queueStartIndex = 0,
            isEndlessQueue = false,
            visualizationMode = com.aethelsoft.grooveplayer.domain.model.VisualizationMode.SIMULATED,
            showMiniPlayerOnStart = false,
            excludedFolders = emptyList()
        )
    }

    override suspend fun updateExcludedFolders(paths: List<String>) {
        val settings = getUserSettings()
        saveUserSettings(settings.copy(excludedFolders = paths))
    }
}
