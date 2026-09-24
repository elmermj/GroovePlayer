package com.aethelsoft.grooveplayer.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
