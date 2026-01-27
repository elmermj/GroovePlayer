package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aethelsoft.grooveplayer.data.local.db.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user_category settings operations.
 * Returns only database entities (never domain models).
 */
@Dao
interface UserSettingsDao {
    
    @Query("SELECT * FROM user_settings LIMIT 1")
    fun observeUserSettings(): Flow<UserSettingsEntity?>
    
    @Query("SELECT * FROM user_settings LIMIT 1")
    suspend fun getUserSettings(): UserSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)
    
    @Update
    suspend fun updateUserSettings(settings: UserSettingsEntity)
    
    @Query("UPDATE user_settings SET lastPlayedSongsTimer = :days WHERE id = 1")
    suspend fun updateLastPlayedSongsTimer(days: Int)
    
    @Query("UPDATE user_settings SET fadeTimer = :seconds WHERE id = 1")
    suspend fun updateFadeTimer(seconds: Int)
    
    @Query("UPDATE user_settings SET equalizerEnabled = :enabled, equalizerPreset = :preset, equalizerBandLevels = :bandLevels WHERE id = 1")
    suspend fun updateEqualizerSettings(enabled: Boolean, preset: Int, bandLevels: String)
    
    @Query("UPDATE user_settings SET visualizationMode = :mode WHERE id = 1")
    suspend fun updateVisualizationMode(mode: String)
    
    @Query("UPDATE user_settings SET lastPlayedSongId = :songId, lastPlayedPosition = :position WHERE id = 1")
    suspend fun updateLastPlayedSong(songId: String?, position: Long)
    
    @Query("UPDATE user_settings SET lastPlayedSongId = :songId, lastPlayedPosition = :position, shuffleEnabled = :shuffle, repeatMode = :repeat, queueSongIds = :queueSongIds, queueStartIndex = :queueStartIndex, isEndlessQueue = :isEndlessQueue WHERE id = 1")
    suspend fun updatePlayerState(
        songId: String?,
        position: Long,
        shuffle: Boolean,
        repeat: String,
        queueSongIds: String,
        queueStartIndex: Int,
        isEndlessQueue: Boolean
    )
    
    @Query("DELETE FROM user_settings")
    suspend fun deleteAllSettings()
    
    @Transaction
    suspend fun resetToDefault() {
        deleteAllSettings()
        insertUserSettings(UserSettingsEntity(
            id = 1, 
            lastPlayedSongsTimer = 30, 
            fadeTimer = 0,
            equalizerEnabled = false,
            equalizerPreset = -1,
            equalizerBandLevels = "",
            lastPlayedSongId = null,
            lastPlayedPosition = 0L,
            shuffleEnabled = false,
            repeatMode = "OFF",
            queueSongIds = "",
            queueStartIndex = 0,
            isEndlessQueue = false
        ))
    }
}
