package com.aethelsoft.grooveplayer.data.backup

import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.local.db.dao.PlaybackHistoryDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongLikeDao
import com.aethelsoft.grooveplayer.data.local.db.dao.SongMetadataDao
import com.aethelsoft.grooveplayer.data.local.db.dao.UserSettingsDao
import com.aethelsoft.grooveplayer.data.local.db.entity.PlaybackHistoryEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongLikeEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.SongMetadataEntity
import com.aethelsoft.grooveplayer.data.mapper.UserMapper
import com.aethelsoft.grooveplayer.domain.backup.restoredMetadata
import com.aethelsoft.grooveplayer.domain.backup.restoredPlayback
import com.aethelsoft.grooveplayer.domain.backup.restoredSettings
import com.aethelsoft.grooveplayer.domain.backup.restoredSongLike
import com.aethelsoft.grooveplayer.domain.backup.restoredSourcePath
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Row copy used before atomic file swap. Restore no longer calls this: the staged
 * snapshot replaces the live database file, then the process restarts onto it.
 * Likes travel inside that file. If this row copy runs, a snapshot without song_likes
 * leaves local likes in place, and a snapshot that has the table replaces them,
 * including an empty table.
 */
@Singleton
class LibrarySnapshotApplier @Inject constructor(
    private val database: GroovePlayerDatabase,
    private val userSettingsDao: UserSettingsDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val songMetadataDao: SongMetadataDao,
    private val songLikeDao: SongLikeDao,
    private val songDao: SongDao,
    private val musicRepository: MusicRepository,
) {
    suspend fun apply(snapshot: File) {
        val tables = readTables(snapshot)
        database.withTransaction {
            if (tables.settings != null) {
                userSettingsDao.deleteAllSettings()
                userSettingsDao.insertUserSettings(
                    UserMapper.userSettingsToEntity(tables.settings.copy(id = 1)),
                )
            }
            tables.history?.let { history ->
                playbackHistoryDao.deleteAll()
                history.forEach { row ->
                    playbackHistoryDao.insertPlayback(
                        PlaybackHistoryEntity(
                            id = 0,
                            songId = row.songId,
                            songTitle = row.songTitle,
                            artist = row.artist,
                            album = row.album,
                            genre = row.genre,
                            uri = row.uri,
                            artworkUrl = row.artworkUrl,
                            playedAt = row.playedAt,
                        ),
                    )
                }
            }
            tables.metadata?.let { metadata ->
                songMetadataDao.deleteAll()
                metadata.forEach { row ->
                    songMetadataDao.insertOrUpdate(
                        SongMetadataEntity(
                            songId = row.songId,
                            title = row.title,
                            genres = row.genres,
                            artists = row.artists,
                            album = row.album,
                            year = row.year,
                            useAlbumYear = row.useAlbumYear,
                            updatedAt = row.updatedAt,
                        ),
                    )
                }
            }
            tables.sourcePaths.forEach { (songId, path) ->
                if (songDao.findSongId(songId) != null) {
                    songDao.updateSourcePath(songId, path)
                }
            }
            tables.likes?.let { likes ->
                songLikeDao.deleteAll()
                likes.forEach { row ->
                    songLikeDao.insert(
                        SongLikeEntity(
                            songId = row.songId,
                            title = row.title,
                            artist = row.artist,
                            album = row.album,
                            genre = row.genre,
                            uri = row.uri,
                            artworkUrl = row.artworkUrl,
                            durationMs = row.durationMs,
                            likedAt = row.likedAt,
                        ),
                    )
                }
            }
        }
        musicRepository.bumpCatalogGeneration()
    }

    private fun readTables(snapshot: File): SnapshotTables {
        val db = SQLiteDatabase.openDatabase(
            snapshot.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        return db.use { sqlite ->
            val settings = readRows(sqlite, "user_settings")?.firstOrNull()?.let(::restoredSettings)
            val history = readRows(sqlite, "playback_history")?.mapNotNull(::restoredPlayback)
            val metadata = readRows(sqlite, "song_metadata")?.mapNotNull(::restoredMetadata)
            val sourcePaths = readRows(sqlite, "songs").orEmpty().mapNotNull(::restoredSourcePath)
            val likes = readRows(sqlite, "song_likes")?.mapNotNull(::restoredSongLike)
            SnapshotTables(settings, history, metadata, sourcePaths, likes)
        }
    }

    /**
     * Null when the table is absent (older snapshot). An empty list means the table exists
     * and the restored library really has no rows.
     */
    private fun readRows(db: SQLiteDatabase, table: String): List<Map<String, String?>>? {
        if (!tableExists(db, table)) return null
        val rows = mutableListOf<Map<String, String?>>()
        db.rawQuery("SELECT * FROM $table", null).use { cursor ->
            while (cursor.moveToNext()) {
                val row = LinkedHashMap<String, String?>(cursor.columnCount)
                for (index in 0 until cursor.columnCount) {
                    row[cursor.getColumnName(index)] =
                        if (cursor.isNull(index)) null else cursor.getString(index)
                }
                rows += row
            }
        }
        return rows
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean {
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table),
        ).use { cursor -> return cursor.moveToFirst() }
    }

    private data class SnapshotTables(
        val settings: com.aethelsoft.grooveplayer.domain.model.UserSettings?,
        val history: List<com.aethelsoft.grooveplayer.domain.backup.RestoredPlayback>?,
        val metadata: List<com.aethelsoft.grooveplayer.domain.backup.RestoredMetadata>?,
        val sourcePaths: List<Pair<String, String>>,
        val likes: List<com.aethelsoft.grooveplayer.domain.backup.RestoredSongLike>?,
    )
}
