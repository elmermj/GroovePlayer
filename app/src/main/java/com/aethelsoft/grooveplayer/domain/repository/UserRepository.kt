package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.UserProfile
import com.aethelsoft.grooveplayer.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user_category profile and settings operations.
 * Defines the contract that the data layer must implement.
 */
interface UserRepository {
    
    // UserProfile operations
    /**
     * Observes the current user_category profile reactively.
     */
    fun observeUserProfile(): Flow<UserProfile?>
    
    /**
     * Gets the current user_category profile (one-time).
     */
    suspend fun getUserProfile(): UserProfile?
    
    /**
     * Creates or updates the user_category profile.
     */
    suspend fun saveUserProfile(profile: UserProfile)
    
    /**
     * Updates specific fields of the user_category profile.
     */
    suspend fun updateUsername(username: String)
    suspend fun updateEmail(email: String)
    suspend fun updateProfilePicture(url: String?)
    suspend fun updatePrivilegeTier(tier: PrivilegeTier)
    
    /**
     * Deletes the user_category profile (logout/account deletion).
     */
    suspend fun deleteUserProfile()
    
    // UserSettings operations
    /**
     * Observes user_category settings reactively.
     */
    fun observeUserSettings(): Flow<UserSettings>
    
    /**
     * Gets the current user_category settings (one-time).
     */
    suspend fun getUserSettings(): UserSettings
    
    /**
     * Saves user_category settings.
     */
    suspend fun saveUserSettings(settings: UserSettings)
    
    /**
     * Updates specific settings.
     */
    suspend fun updateLastPlayedSongsTimer(days: Int)
    suspend fun updateFadeTimer(seconds: Int)
    suspend fun updateEqualizerSettings(
        enabled: Boolean,
        preset: Int,
        bandLevels: List<Int>
    )
    suspend fun updateVisualizationMode(mode: com.aethelsoft.grooveplayer.domain.model.VisualizationMode)
    suspend fun updateRepeatAndShuffle(shuffle: Boolean, repeat: String)
    suspend fun updateShowMiniPlayerOnStart(enabled: Boolean)
    /**
     * Replaces the list of folder paths excluded from music scanning.
     */
    suspend fun updateExcludedFolders(paths: List<String>)
    suspend fun updateLastPlayedSong(songId: String?, position: Long)
    suspend fun updatePlayerState(
        songId: String?,
        position: Long,
        shuffle: Boolean,
        repeat: String,
        queueSongIds: List<String>,
        queueStartIndex: Int,
        isEndlessQueue: Boolean
    )
    
    /**
     * Resets settings to defaults.
     */
    suspend fun resetSettingsToDefault()
}
