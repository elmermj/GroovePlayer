package com.aethelsoft.grooveplayer.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds [song_likes] without rewriting the rest of the library file.
 * Backup gzips and hashes that whole file, so likes travel with the snapshot.
 * Restore swaps in the staged file (SCRUM-67). An older snapshot (schema 16, no
 * song_likes) runs this migration on open and gets an empty table. A snapshot that
 * already has song_likes keeps those rows because they are inside the swapped file.
 */
object GroovePlayerMigrations {
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
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
                """.trimIndent(),
            )
        }
    }

    /**
     * User playlists (SCRUM-9). Creates empty tables on a schema 17 library and
     * leaves song_likes and every other row in place.
     *
     * Also adds songs.sourcePath when a database reached version 17 without it
     * (the playlists draft numbered its own schema 16, before test's sourcePath
     * column). Test databases already have the column, so this is a no-op there.
     */
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!hasColumn(db, "songs", "sourcePath")) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `sourcePath` TEXT")
            }
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlist_tracks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `playlistId` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    `songId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `uri` TEXT NOT NULL,
                    `filePath` TEXT,
                    `durationMs` INTEGER NOT NULL,
                    `artworkUrl` TEXT,
                    `albumName` TEXT,
                    `genre` TEXT NOT NULL,
                    FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId` ON `playlist_tracks` (`playlistId`)",
            )
        }
    }

    private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }
}
