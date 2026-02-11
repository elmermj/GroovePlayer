package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.GenreEntity

@Dao
interface GenreDao {

    @Query("SELECT * FROM genres WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): GenreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: GenreEntity): Long

    @Query("SELECT name FROM genres WHERE name LIKE :query || '%' ORDER BY name ASC")
    suspend fun searchGenres(query: String): List<String>

    @Query("SELECT name FROM genres ORDER BY name ASC")
    suspend fun getAllGenres(): List<String>
}

