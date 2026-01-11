package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lastPlayedSongsTimer: Int = 0,
    val fadeTimer: Int = 0
)
