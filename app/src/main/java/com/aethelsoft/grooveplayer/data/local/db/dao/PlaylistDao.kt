package com.aethelsoft.grooveplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistTrackEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaylistWithTracksRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PlaylistWithTracksRelation>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistWithTracksRelation?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistWithTracksRelation?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert
    suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE id = :entryId")
    suspend fun deleteTrack(entryId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracksFor(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC, id ASC")
    suspend fun tracksFor(playlistId: Long): List<PlaylistTrackEntity>

    @Query("UPDATE playlist_tracks SET position = :position WHERE id = :entryId")
    suspend fun updatePosition(entryId: Long, position: Int)
}
