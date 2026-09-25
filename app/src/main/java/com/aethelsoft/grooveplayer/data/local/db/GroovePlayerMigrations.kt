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
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySchemaMigration.STATEMENTS.forEach {
                db.execSQL(it)
            }
        }
    }

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
     * User playlists. Schema 18 is SCRUM-84 (songs.contentHash). This step only
     * adds tables, keyed by that hash. Renumber [MIGRATION_18_19] with
     * [GroovePlayerDatabase] if another migration lands on 18 first.
     */
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            com.aethelsoft.grooveplayer.domain.playlist.PlaylistSchemaMigration.STATEMENTS.forEach {
                db.execSQL(it)
            }
        }
    }
}
