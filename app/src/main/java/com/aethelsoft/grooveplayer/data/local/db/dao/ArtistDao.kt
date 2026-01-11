package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.*
import com.aethelsoft.grooveplayer.data.local.db.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists WHERE name LIKE :query || '%' ORDER BY name ASC")
    suspend fun searchArtists(query: String): List<ArtistEntity>
    
    @Query("SELECT DISTINCT name FROM artists ORDER BY name ASC")
    suspend fun getAllArtistNames(): List<String>
    
    @Query("SELECT * FROM artists WHERE name = :name")
    suspend fun getArtist(name: String): ArtistEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(artist: ArtistEntity)
    
    @Query("SELECT DISTINCT artist FROM playback_history WHERE artist LIKE :query || '%' ORDER BY artist ASC")
    suspend fun searchArtistsFromHistory(query: String): List<String>
}

