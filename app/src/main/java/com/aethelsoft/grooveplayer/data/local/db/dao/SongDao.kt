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

    @Query("DELETE FROM song_genres WHERE songId = :songId")
    suspend fun deleteSongGenreCrossRefs(songId: String)

    @Query("SELECT * FROM songs WHERE songId = :songId")
    suspend fun getSong(songId: String): SongEntity?

    @Query("SELECT songId FROM songs WHERE songId = :songId LIMIT 1")
    suspend fun findSongId(songId: String): String?

    @Query("SELECT sourcePath FROM songs WHERE songId = :songId")
    suspend fun getSourcePath(songId: String): String?

    @Query("DELETE FROM songs WHERE songId IN (:songIds)")
    suspend fun deleteBySongIds(songIds: List<String>)
}

