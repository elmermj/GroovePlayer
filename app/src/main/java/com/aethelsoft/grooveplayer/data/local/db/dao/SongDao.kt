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

    @Query("UPDATE songs SET sourcePath = :sourcePath WHERE songId = :songId")
    suspend fun updateSourcePath(songId: String, sourcePath: String)

    /** Rows whose stored path matched. Callers delete the old file only when this is > 0. */
    @Query("UPDATE songs SET sourcePath = :newPath WHERE sourcePath = :oldPath")
    suspend fun retargetSourcePath(oldPath: String, newPath: String): Int

    @Query("DELETE FROM songs WHERE songId IN (:songIds)")
    suspend fun deleteBySongIds(songIds: List<String>)

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE sourcePath = :path LIMIT 1")
    suspend fun findBySourcePath(path: String): SongEntity?

    @Query("SELECT * FROM songs WHERE contentHash = :hash COLLATE NOCASE")
    suspend fun findByContentHash(hash: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE contentHash IS NULL OR contentHash = ''")
    suspend fun songsMissingHash(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs WHERE inPrivateLibrary = 0")
    suspend fun countHidden(): Int

    @Query("UPDATE songs SET contentHash = :contentHash WHERE songId = :songId")
    suspend fun updateContentHash(songId: String, contentHash: String)

    @Query("UPDATE songs SET inPrivateLibrary = :inPrivateLibrary WHERE songId = :songId")
    suspend fun setPrivateFlag(songId: String, inPrivateLibrary: Boolean)

    @Query(
        """
        UPDATE songs
        SET sourcePath = :sourcePath,
            uri = :uri,
            title = :title,
            durationMs = :durationMs,
            contentHash = :contentHash,
            inPrivateLibrary = 1
        WHERE songId = :songId
        """
    )
    suspend fun adoptPrivateFile(
        songId: String,
        sourcePath: String,
        uri: String,
        title: String,
        durationMs: Long,
        contentHash: String,
    )

    @Query(
        """
        INSERT OR IGNORE INTO song_artists (songId, artistId)
        SELECT :toId, artistId FROM song_artists WHERE songId = :fromId
        """
    )
    suspend fun copyArtistLinks(fromId: String, toId: String)

    @Query("DELETE FROM song_artists WHERE songId = :songId")
    suspend fun deleteArtistLinks(songId: String)

    @Query(
        """
        INSERT OR IGNORE INTO song_genres (songId, genreId)
        SELECT :toId, genreId FROM song_genres WHERE songId = :fromId
        """
    )
    suspend fun copyGenreLinks(fromId: String, toId: String)

    @Query("DELETE FROM song_genres WHERE songId = :songId")
    suspend fun deleteGenreLinks(songId: String)
}

