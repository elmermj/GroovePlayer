package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.SongArtistCrossRef
import com.aethelsoft.grooveplayer.data.local.db.entity.SongEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongGenreCrossRef

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongArtistCrossRef(crossRef: SongArtistCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongGenreCrossRef(crossRef: SongGenreCrossRef)

    @Query("SELECT * FROM songs WHERE songId = :songId")
    suspend fun getSong(songId: String): SongEntity?
}

