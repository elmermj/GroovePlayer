package com.aethelsoft.grooveplayer.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySchemaMigration
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistSchemaMigration

/**
 * Non-destructive path from schema 1 through [GroovePlayerDatabase.SCHEMA_VERSION].
 * Versions 1–3 were never released. 5–8, 10–11, and 13–14 were skipped in git.
 * Each step runs the same idempotent upgrade: create missing tables, add missing
 * columns, and copy albums/artists into the relational shape. Existing rows stay.
 * Playback history, likes, playlists, and play counts are never deleted.
 */
object GroovePlayerMigrations {
    const val OLDEST_VERSION = 1

    val ALL: List<Migration> = (OLDEST_VERSION until GroovePlayerDatabase.SCHEMA_VERSION).map { from ->
        object : Migration(from, from + 1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                upgrade(db)
            }
        }
    }

    /** Every statement an upgrade may run. Tests reject DROP and DELETE FROM. */
    fun auditedSql(): List<String> = buildList {
        addAll(USER_TABLES)
        add(SEARCH_HISTORY)
        addAll(ALBUM_RESHAPE)
        addAll(ARTIST_RESHAPE)
        addAll(RELATIONAL)
        add(INSERT_ARTISTS_FROM_ALBUMS)
        add(LINK_ALBUM_ARTISTS)
        add(CREATE_USER_SETTINGS)
        addAll(SETTINGS_ADDS.map { (_, ddl) -> "ALTER TABLE `user_settings` ADD COLUMN $ddl" })
        addAll(TRANSFERS)
        addAll(SONG_ADDS.map { (_, ddl) -> "ALTER TABLE `songs` ADD COLUMN $ddl" })
        add(SONG_LIKES)
        addAll(PlaylistSchemaMigration.STATEMENTS)
        addAll(PrivateLibrarySchemaMigration.STATEMENTS)
    }

    private fun upgrade(db: SupportSQLiteDatabase) {
        USER_TABLES.forEach { db.execSQL(it) }
        db.execSQL(SEARCH_HISTORY)
        val albumsReshaped = reshapeAlbums(db)
        reshapeArtists(db)
        RELATIONAL.forEach { db.execSQL(it) }
        if (albumsReshaped && tableExists(db, "albums_preserved") &&
            "artist" in columns(db, "albums_preserved")
        ) {
            db.execSQL(INSERT_ARTISTS_FROM_ALBUMS)
            db.execSQL(LINK_ALBUM_ARTISTS)
        }
        if (!tableExists(db, "user_settings")) {
            db.execSQL(CREATE_USER_SETTINGS)
        } else {
            addMissing(db, "user_settings", SETTINGS_ADDS)
        }
        TRANSFERS.forEach { db.execSQL(it) }
        if (tableExists(db, "songs")) {
            addMissing(db, "songs", SONG_ADDS)
        }
        db.execSQL(SONG_LIKES)
        PlaylistSchemaMigration.STATEMENTS.forEach { db.execSQL(it) }
    }

    private fun reshapeAlbums(db: SupportSQLiteDatabase): Boolean {
        if (!tableExists(db, "albums")) {
            db.execSQL(CREATE_ALBUMS)
            db.execSQL(INDEX_ALBUMS)
            return false
        }
        if ("albumId" in columns(db, "albums")) return false
        db.execSQL("ALTER TABLE `albums` RENAME TO `albums_preserved`")
        db.execSQL(CREATE_ALBUMS)
        db.execSQL(INDEX_ALBUMS)
        db.execSQL(COPY_ALBUMS)
        return true
    }

    private fun reshapeArtists(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "artists")) {
            db.execSQL(CREATE_ARTISTS)
            db.execSQL(INDEX_ARTISTS)
            return
        }
        if ("artistId" in columns(db, "artists")) return
        db.execSQL("ALTER TABLE `artists` RENAME TO `artists_preserved`")
        db.execSQL(CREATE_ARTISTS)
        db.execSQL(INDEX_ARTISTS)
        db.execSQL(COPY_ARTISTS)
    }

    private fun addMissing(
        db: SupportSQLiteDatabase,
        table: String,
        columns: List<Pair<String, String>>,
    ) {
        val have = columns(db, table)
        columns.forEach { (name, ddl) ->
            if (name !in have) db.execSQL("ALTER TABLE `$table` ADD COLUMN $ddl")
        }
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table' LIMIT 1",
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val names = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val index = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                names += cursor.getString(index)
            }
        }
        return names
    }

    private val USER_TABLES = listOf(
        """
        CREATE TABLE IF NOT EXISTS `playback_history` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `songId` TEXT NOT NULL,
            `songTitle` TEXT NOT NULL,
            `artist` TEXT NOT NULL,
            `album` TEXT NOT NULL,
            `genre` TEXT NOT NULL,
            `uri` TEXT NOT NULL,
            `artworkUrl` TEXT,
            `playedAt` INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `song_metadata` (
            `songId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `genres` TEXT NOT NULL,
            `artists` TEXT NOT NULL,
            `album` TEXT,
            `year` INTEGER,
            `useAlbumYear` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`songId`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `user_profile` (
            `id` TEXT NOT NULL,
            `username` TEXT NOT NULL,
            `email` TEXT NOT NULL,
            `profile_picture_url` TEXT,
            `privilege_tier` TEXT NOT NULL,
            `settings_references` TEXT NOT NULL,
            `created_at` INTEGER NOT NULL,
            `updated_at` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )

    private const val SEARCH_HISTORY = """
        CREATE TABLE IF NOT EXISTS `search_history` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `type` TEXT NOT NULL,
            `query` TEXT,
            `itemId` TEXT,
            `itemTitle` TEXT NOT NULL,
            `itemSubtitle` TEXT,
            `artworkUrl` TEXT,
            `timestamp` INTEGER NOT NULL
        )
    """

    private const val CREATE_ALBUMS = """
        CREATE TABLE IF NOT EXISTS `albums` (
            `albumId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `artworkUrl` TEXT,
            `year` INTEGER,
            `updatedAt` INTEGER NOT NULL
        )
    """

    private const val INDEX_ALBUMS =
        "CREATE INDEX IF NOT EXISTS `index_albums_name` ON `albums` (`name`)"

    private const val COPY_ALBUMS = """
        INSERT INTO `albums` (`name`, `artworkUrl`, `year`, `updatedAt`)
        SELECT `name`, `artworkUrl`, `year`, `updatedAt` FROM `albums_preserved`
    """

    private val ALBUM_RESHAPE = listOf(
        "ALTER TABLE `albums` RENAME TO `albums_preserved`",
        CREATE_ALBUMS,
        INDEX_ALBUMS,
        COPY_ALBUMS,
    )

    private const val CREATE_ARTISTS = """
        CREATE TABLE IF NOT EXISTS `artists` (
            `artistId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `genre` TEXT,
            `desc` TEXT,
            `country` TEXT,
            `artist_image_url` TEXT,
            `updatedAt` INTEGER NOT NULL
        )
    """

    private const val INDEX_ARTISTS =
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_artists_name` ON `artists` (`name`)"

    private const val COPY_ARTISTS = """
        INSERT INTO `artists` (`name`, `artist_image_url`, `updatedAt`)
        SELECT `name`, `imageUrl`, `updatedAt` FROM `artists_preserved`
    """

    private val ARTIST_RESHAPE = listOf(
        "ALTER TABLE `artists` RENAME TO `artists_preserved`",
        CREATE_ARTISTS,
        INDEX_ARTISTS,
        COPY_ARTISTS,
    )

    private const val INSERT_ARTISTS_FROM_ALBUMS = """
        INSERT INTO `artists` (`name`, `updatedAt`)
        SELECT p.`artist`, MAX(p.`updatedAt`)
        FROM `albums_preserved` AS p
        WHERE p.`artist` IS NOT NULL AND p.`artist` != ''
        AND NOT EXISTS (SELECT 1 FROM `artists` AS a WHERE a.`name` = p.`artist`)
        GROUP BY p.`artist`
    """

    private const val LINK_ALBUM_ARTISTS = """
        INSERT INTO `album_artists` (`albumId`, `artistId`)
        SELECT a.`albumId`, ar.`artistId`
        FROM `albums_preserved` AS old
        JOIN `albums` AS a ON a.`name` = old.`name`
        JOIN `artists` AS ar ON ar.`name` = old.`artist`
        WHERE old.`artist` IS NOT NULL AND old.`artist` != ''
    """

    private val RELATIONAL = listOf(
        """
        CREATE TABLE IF NOT EXISTS `genres` (
            `genreId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_genres_name` ON `genres` (`name`)",
        CREATE_ALBUMS,
        INDEX_ALBUMS,
        CREATE_ARTISTS,
        INDEX_ARTISTS,
        """
        CREATE TABLE IF NOT EXISTS `songs` (
            `songId` TEXT NOT NULL,
            `albumId` INTEGER,
            `uri` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `trackNumber` INTEGER,
            `durationMs` INTEGER,
            PRIMARY KEY(`songId`),
            FOREIGN KEY(`albumId`) REFERENCES `albums`(`albumId`) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_songs_albumId` ON `songs` (`albumId`)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_uri` ON `songs` (`uri`)",
        """
        CREATE TABLE IF NOT EXISTS `album_artists` (
            `albumId` INTEGER NOT NULL,
            `artistId` INTEGER NOT NULL,
            PRIMARY KEY(`albumId`, `artistId`),
            FOREIGN KEY(`albumId`) REFERENCES `albums`(`albumId`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`artistId`) REFERENCES `artists`(`artistId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_album_artists_albumId` ON `album_artists` (`albumId`)",
        "CREATE INDEX IF NOT EXISTS `index_album_artists_artistId` ON `album_artists` (`artistId`)",
        """
        CREATE TABLE IF NOT EXISTS `song_artists` (
            `songId` TEXT NOT NULL,
            `artistId` INTEGER NOT NULL,
            PRIMARY KEY(`songId`, `artistId`),
            FOREIGN KEY(`songId`) REFERENCES `songs`(`songId`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`artistId`) REFERENCES `artists`(`artistId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_song_artists_songId` ON `song_artists` (`songId`)",
        "CREATE INDEX IF NOT EXISTS `index_song_artists_artistId` ON `song_artists` (`artistId`)",
        """
        CREATE TABLE IF NOT EXISTS `song_genres` (
            `songId` TEXT NOT NULL,
            `genreId` INTEGER NOT NULL,
            PRIMARY KEY(`songId`, `genreId`),
            FOREIGN KEY(`songId`) REFERENCES `songs`(`songId`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`genreId`) REFERENCES `genres`(`genreId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_song_genres_songId` ON `song_genres` (`songId`)",
        "CREATE INDEX IF NOT EXISTS `index_song_genres_genreId` ON `song_genres` (`genreId`)",
    )

    private const val CREATE_USER_SETTINGS = """
        CREATE TABLE IF NOT EXISTS `user_settings` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `lastPlayedSongsTimer` INTEGER NOT NULL,
            `fadeTimer` INTEGER NOT NULL,
            `equalizerEnabled` INTEGER NOT NULL,
            `equalizerPreset` INTEGER NOT NULL,
            `equalizerBandLevels` TEXT NOT NULL,
            `lastPlayedSongId` TEXT,
            `lastPlayedPosition` INTEGER NOT NULL,
            `shuffleEnabled` INTEGER NOT NULL,
            `repeatMode` TEXT NOT NULL,
            `queueSongIds` TEXT NOT NULL,
            `queueStartIndex` INTEGER NOT NULL,
            `isEndlessQueue` INTEGER NOT NULL,
            `visualizationMode` TEXT NOT NULL,
            `showMiniPlayerOnStart` INTEGER NOT NULL,
            `notificationsEnabled` INTEGER NOT NULL,
            `excludedFolders` TEXT NOT NULL,
            `uiStyleId` TEXT NOT NULL,
            `uiStyleOverrides` TEXT NOT NULL
        )
    """

    private val SETTINGS_ADDS = listOf(
        "equalizerEnabled" to "`equalizerEnabled` INTEGER NOT NULL DEFAULT 0",
        "equalizerPreset" to "`equalizerPreset` INTEGER NOT NULL DEFAULT -1",
        "equalizerBandLevels" to "`equalizerBandLevels` TEXT NOT NULL DEFAULT ''",
        "lastPlayedSongId" to "`lastPlayedSongId` TEXT",
        "lastPlayedPosition" to "`lastPlayedPosition` INTEGER NOT NULL DEFAULT 0",
        "shuffleEnabled" to "`shuffleEnabled` INTEGER NOT NULL DEFAULT 0",
        "repeatMode" to "`repeatMode` TEXT NOT NULL DEFAULT 'OFF'",
        "queueSongIds" to "`queueSongIds` TEXT NOT NULL DEFAULT ''",
        "queueStartIndex" to "`queueStartIndex` INTEGER NOT NULL DEFAULT 0",
        "isEndlessQueue" to "`isEndlessQueue` INTEGER NOT NULL DEFAULT 0",
        "visualizationMode" to "`visualizationMode` TEXT NOT NULL DEFAULT 'SIMULATED'",
        "showMiniPlayerOnStart" to "`showMiniPlayerOnStart` INTEGER NOT NULL DEFAULT 0",
        "notificationsEnabled" to "`notificationsEnabled` INTEGER NOT NULL DEFAULT 1",
        "excludedFolders" to "`excludedFolders` TEXT NOT NULL DEFAULT ''",
        "uiStyleId" to "`uiStyleId` TEXT NOT NULL DEFAULT 'default'",
        "uiStyleOverrides" to "`uiStyleOverrides` TEXT NOT NULL DEFAULT ''",
    )

    private val SONG_ADDS = listOf(
        "sourcePath" to "`sourcePath` TEXT",
        "contentHash" to "`contentHash` TEXT",
        "inPrivateLibrary" to "`inPrivateLibrary` INTEGER NOT NULL DEFAULT 0",
    )

    private val TRANSFERS = listOf(
        """
        CREATE TABLE IF NOT EXISTS `transfers` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `deviceName` TEXT NOT NULL,
            `startTime` INTEGER NOT NULL,
            `endTime` INTEGER,
            `totalBytes` INTEGER NOT NULL,
            `transferredBytes` INTEGER NOT NULL,
            `overallStatus` TEXT NOT NULL,
            `isSender` INTEGER NOT NULL
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_transfers_overallStatus` ON `transfers` (`overallStatus`)",
        """
        CREATE TABLE IF NOT EXISTS `transfer_files` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `transferId` INTEGER NOT NULL,
            `fileName` TEXT NOT NULL,
            `filePath` TEXT NOT NULL,
            `fileSize` INTEGER NOT NULL,
            `transferredBytes` INTEGER NOT NULL,
            `checksum` TEXT,
            `status` TEXT NOT NULL,
            `retryCount` INTEGER NOT NULL,
            FOREIGN KEY(`transferId`) REFERENCES `transfers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_transfer_files_transferId` ON `transfer_files` (`transferId`)",
    )

    private const val SONG_LIKES = """
        CREATE TABLE IF NOT EXISTS `song_likes` (
            `songId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `artist` TEXT NOT NULL,
            `album` TEXT NOT NULL,
            `genre` TEXT NOT NULL,
            `uri` TEXT NOT NULL,
            `artworkUrl` TEXT,
            `durationMs` INTEGER NOT NULL,
            `likedAt` INTEGER NOT NULL,
            PRIMARY KEY(`songId`)
        )
    """
}
