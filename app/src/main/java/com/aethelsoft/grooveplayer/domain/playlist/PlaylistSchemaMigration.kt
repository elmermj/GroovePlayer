package com.aethelsoft.grooveplayer.domain.playlist

/**
 * Room 18 → 19. Adds playlist tables only. Rows are keyed by
 * [songs.contentHash] from SCRUM-84. No existing table is rewritten.
 */
object PlaylistSchemaMigration {
    val STATEMENTS = listOf(
        """
        CREATE TABLE IF NOT EXISTS `playlists` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `playlist_tracks` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `playlistId` INTEGER NOT NULL,
            `position` INTEGER NOT NULL,
            `contentHash` TEXT NOT NULL,
            `songId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `artist` TEXT NOT NULL,
            `durationMs` INTEGER NOT NULL,
            `artworkUrl` TEXT,
            `albumName` TEXT,
            `genre` TEXT NOT NULL,
            FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId` ON `playlist_tracks` (`playlistId`)",
        "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_contentHash` ON `playlist_tracks` (`contentHash`)",
    )
}
