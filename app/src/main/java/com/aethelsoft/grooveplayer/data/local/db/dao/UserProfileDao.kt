package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aethelsoft.grooveplayer.data.local.db.entity.UserProfileEntity
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user_category profile operations.
 * Returns only database entities (never domain models).
 */
@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)
    
    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)
    
    @Query("UPDATE user_profile SET username = :username, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updateUsername(userId: String, username: String, updatedAt: Long)
    
    @Query("UPDATE user_profile SET email = :email, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updateEmail(userId: String, email: String, updatedAt: Long)
    
    @Query("UPDATE user_profile SET profile_picture_url = :url, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updateProfilePicture(userId: String, url: String?, updatedAt: Long)
    
    @Query("UPDATE user_profile SET privilege_tier = :tier, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updatePrivilegeTier(userId: String, tier: PrivilegeTier, updatedAt: Long)
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteUserProfile()
}
