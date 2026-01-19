package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.AlbumEntity

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums WHERE name LIKE :query || '%' ORDER BY name ASC")
    suspend fun searchAlbums(query: String): List<AlbumEntity>
    
    @Query("SELECT DISTINCT name FROM albums ORDER BY name ASC")
    suspend fun getAllAlbumNames(): List<String>
    
    @Query("SELECT * FROM albums WHERE name = :name AND artist = :artist")
    suspend fun getAlbum(name: String, artist: String): AlbumEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(album: AlbumEntity)
    
    @Query("SELECT DISTINCT album FROM playback_history WHERE album LIKE :query || '%' ORDER BY album ASC")
    suspend fun searchAlbumsFromHistory(query: String): List<String>
}

