package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier

/**
 * Database entity for user_category profile.
 * Data layer model - Room specific annotations.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    
    @ColumnInfo(name = "username")
    val username: String,
    
    @ColumnInfo(name = "email")
    val email: String,
    
    @ColumnInfo(name = "profile_picture_url")
    val profilePictureUrl: String? = null,
    
    @ColumnInfo(name = "privilege_tier")
    val privilegeTier: PrivilegeTier = PrivilegeTier.FREE,
    
    @ColumnInfo(name = "settings_references")
    val settingsReferences: String = "",  // Stored as comma-separated string
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,  // Timestamp in milliseconds
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long   // Timestamp in milliseconds
)
