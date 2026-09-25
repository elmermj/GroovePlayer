package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.SongLikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongLikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(like: SongLikeEntity)

    @Query("DELETE FROM song_likes WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM song_likes WHERE songId IN (:songIds)")
    suspend fun deleteBySongIds(songIds: List<String>)

    @Query("DELETE FROM song_likes")
    suspend fun deleteAll()

    @Query("SELECT songId FROM song_likes WHERE songId = :songId LIMIT 1")
    suspend fun findSongId(songId: String): String?

    @Query("SELECT songId FROM song_likes")
    fun observeSongIds(): Flow<List<String>>

    @Query("SELECT * FROM song_likes")
    fun observeAll(): Flow<List<SongLikeEntity>>
}
