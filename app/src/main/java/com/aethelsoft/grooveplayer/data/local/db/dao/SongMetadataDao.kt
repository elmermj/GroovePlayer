package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.*
import com.aethelsoft.grooveplayer.data.local.db.entity.SongMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongMetadataDao {
    @Query("SELECT * FROM song_metadata WHERE songId = :songId")
    suspend fun getMetadata(songId: String): SongMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SongMetadataEntity)
    
    @Delete
    suspend fun delete(metadata: SongMetadataEntity)
    
    @Query("SELECT * FROM song_metadata")
    fun getAllMetadata(): Flow<List<SongMetadataEntity>>
}

