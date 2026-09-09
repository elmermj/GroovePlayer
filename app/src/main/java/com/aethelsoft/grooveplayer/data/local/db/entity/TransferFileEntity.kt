package com.aethelsoft.grooveplayer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for files within a transfer.
 */
@Entity(
    tableName = "transfer_files",
    foreignKeys = [
        ForeignKey(
            entity = TransferEntity::class,
            parentColumns = ["id"],
            childColumns = ["transferId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["transferId"])],
)
data class TransferFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transferId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val transferredBytes: Long,
    val checksum: String?,
    val status: String,
    val retryCount: Int,
)
