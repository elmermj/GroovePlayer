package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.*
import androidx.room.Insert
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayback(playback: PlaybackHistoryEntity): Long
    
    @Query("""
        SELECT * FROM playback_history p1
        WHERE playedAt = (
            SELECT MAX(playedAt) 
            FROM playback_history p2 
            WHERE p2.songId = p1.songId
        )
        ORDER BY playedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<PlaybackHistoryEntity>>
    
    @Query("""
        SELECT songId, songTitle, artist, album, genre, uri, artworkUrl, COUNT(*) as playCount
        FROM playback_history
        WHERE playedAt >= :sinceTimestamp
        GROUP BY songId
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getFavoriteTracks(
        sinceTimestamp: Long,
        limit: Int = 50
    ): List<FavoriteTrackResult>
    
    @Query("""
        SELECT artist, COUNT(*) as playCount
        FROM playback_history
        WHERE playedAt >= :sinceTimestamp
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getFavoriteArtists(
        sinceTimestamp: Long,
        limit: Int = 50
    ): List<FavoriteArtistResult>
    
    @Query("""
        SELECT album, artist, COUNT(*) as playCount
        FROM playback_history
        WHERE playedAt >= :sinceTimestamp
        GROUP BY album, artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getFavoriteAlbums(
        sinceTimestamp: Long,
        limit: Int = 50
    ): List<FavoriteAlbumResult>

    @Query("""
        SELECT * FROM playback_history
        WHERE playedAt >= :sinceTimestamp
        ORDER BY playedAt DESC
        LIMIT :limit
    """)
    suspend fun getLastPlayedSongs(
        sinceTimestamp: Long,
        limit: Int = 8
    ): List<PlaybackHistoryEntity>
    
    @Query("DELETE FROM playback_history WHERE playedAt < :beforeTimestamp")
    suspend fun deleteOldHistory(beforeTimestamp: Long)
    
    @Query("SELECT COUNT(*) FROM playback_history")
    suspend fun getTotalPlaybackCount(): Int
    
    @Query("SELECT DISTINCT genre FROM playback_history WHERE genre IS NOT NULL AND genre != '' ORDER BY genre ASC")
    suspend fun getAllGenres(): List<String>
    
    @Query("SELECT DISTINCT genre FROM playback_history WHERE genre LIKE :query || '%' AND genre IS NOT NULL AND genre != '' ORDER BY genre ASC")
    suspend fun searchGenres(query: String): List<String>

    @Query("SELECT DISTINCT artist FROM playback_history WHERE artist LIKE :query || '%' AND artist IS NOT NULL AND artist != '' ORDER BY artist ASC")
    suspend fun searchArtistsFromHistory(query: String): List<String>
}

data class FavoriteTrackResult(
    val songId: String,
    val songTitle: String,
    val artist: String,
    val album: String,
    val genre: String,
    val uri: String,
    val artworkUrl: String?,
    val playCount: Int
)

data class FavoriteArtistResult(
    val artist: String,
    val playCount: Int
)

data class FavoriteAlbumResult(
    val album: String,
    val artist: String,
    val playCount: Int
)

