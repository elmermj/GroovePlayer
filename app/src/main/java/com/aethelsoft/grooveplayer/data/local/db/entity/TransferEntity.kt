package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for transfer sessions.
 * Persists active, completed, failed, and cancelled transfers.
 */
@Entity(
    tableName = "transfers",
    indices = [Index(value = ["overallStatus"])],
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val startTime: Long,
    val endTime: Long?,
    val totalBytes: Long,
    val transferredBytes: Long,
    val overallStatus: String,
    val isSender: Boolean,
)
