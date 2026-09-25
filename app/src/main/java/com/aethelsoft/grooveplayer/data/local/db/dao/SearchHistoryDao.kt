package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aethelsoft.grooveplayer.data.local.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSearchesSync(limit: Int = 10): List<SearchHistoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(history: SearchHistoryEntity)
    
    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistory(id: Long)
    
    @Query("DELETE FROM search_history")
    suspend fun clearAll()

    @Query("DELETE FROM search_history WHERE type = 'SONG' AND itemId IN (:songIds)")
    suspend fun deleteSongEntries(songIds: List<String>)

    @Query("SELECT itemId FROM search_history WHERE type = 'SONG' AND itemId IS NOT NULL")
    suspend fun songItemIds(): List<String>

    @Query("UPDATE search_history SET itemId = :toId WHERE type = 'SONG' AND itemId = :fromId")
    suspend fun retargetSong(fromId: String, toId: String)
    
    @Query("SELECT * FROM search_history WHERE type = 'QUERY' AND query LIKE :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun searchQueries(query: String, limit: Int = 5): List<SearchHistoryEntity>
}
