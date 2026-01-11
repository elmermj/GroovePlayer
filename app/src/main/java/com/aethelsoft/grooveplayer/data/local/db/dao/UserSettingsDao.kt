package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.*
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
    
    @Query("DELETE FROM user_settings")
    suspend fun deleteAllSettings()
    
    @Transaction
    suspend fun resetToDefault() {
        deleteAllSettings()
        insertUserSettings(UserSettingsEntity(id = 1, lastPlayedSongsTimer = 30, fadeTimer = 0))
    }
}
