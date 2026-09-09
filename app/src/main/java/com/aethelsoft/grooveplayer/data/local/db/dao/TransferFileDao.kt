package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.TransferFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: TransferFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<TransferFileEntity>)

    @Query("UPDATE transfer_files SET transferredBytes = :bytes, status = :status, retryCount = :retryCount WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, status: String, retryCount: Int)

    @Query("SELECT * FROM transfer_files WHERE transferId = :transferId ORDER BY id")
    suspend fun getByTransferId(transferId: Long): List<TransferFileEntity>

    @Query("SELECT * FROM transfer_files WHERE transferId = :transferId ORDER BY id")
    fun observeByTransferId(transferId: Long): Flow<List<TransferFileEntity>>

    @Query("SELECT * FROM transfer_files WHERE id = :id")
    suspend fun getById(id: Long): TransferFileEntity?

    @Query("UPDATE transfer_files SET status = :status WHERE status IN ('PENDING','CONNECTING','TRANSFERRING','PAUSED','RETRYING','CHECKSUM_VALIDATING')")
    suspend fun terminateAllActive(status: String)
}
