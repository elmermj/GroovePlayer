package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferEntity): Long

    @Query("UPDATE transfers SET transferredBytes = :bytes, overallStatus = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, status: String)

    @Query("UPDATE transfers SET endTime = :endTime, overallStatus = :status WHERE id = :id")
    suspend fun complete(id: Long, endTime: Long, status: String)

    @Query("UPDATE transfers SET overallStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE transfers SET deviceName = :deviceName WHERE id = :id")
    suspend fun updateDeviceName(id: Long, deviceName: String)

    @Query("UPDATE transfers SET totalBytes = :totalBytes WHERE id = :id")
    suspend fun updateTotalBytes(id: Long, totalBytes: Long)

    @Query("SELECT * FROM transfers WHERE overallStatus IN ('PENDING','CONNECTING','TRANSFERRING','PAUSED','RETRYING','CHECKSUM_VALIDATING') ORDER BY startTime DESC")
    suspend fun getActiveTransfers(): List<TransferEntity>

    @Query("UPDATE transfers SET overallStatus = :status, endTime = :endTime WHERE overallStatus IN ('PENDING','CONNECTING','TRANSFERRING','PAUSED','RETRYING','CHECKSUM_VALIDATING')")
    suspend fun terminateAllActive(status: String, endTime: Long)

    @Query("SELECT * FROM transfers WHERE overallStatus IN ('COMPLETED','FAILED','CANCELLED') ORDER BY startTime DESC")
    suspend fun getTransferHistory(): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getById(id: Long): TransferEntity?

    @Query("SELECT * FROM transfers ORDER BY startTime DESC")
    suspend fun getAll(): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE overallStatus IN ('PENDING','CONNECTING','TRANSFERRING','PAUSED','RETRYING','CHECKSUM_VALIDATING') ORDER BY startTime DESC")
    fun observeActiveTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE overallStatus IN ('COMPLETED','FAILED','CANCELLED') ORDER BY startTime DESC")
    fun observeTransferHistory(): Flow<List<TransferEntity>>
}
