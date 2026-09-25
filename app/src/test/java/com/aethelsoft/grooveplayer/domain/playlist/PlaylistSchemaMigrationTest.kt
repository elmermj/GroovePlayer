package com.aethelsoft.grooveplayer.domain.playlist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSchemaMigrationTest {

    @Test
    fun playlistMigrationOnlyAddsTablesKeyedByContentHash() {
        val sql = PlaylistSchemaMigration.STATEMENTS.joinToString("\n")
        assertFalse(sql.contains("DROP", ignoreCase = true))
        assertFalse(sql.contains("DELETE FROM", ignoreCase = true))
        assertTrue(sql.contains("contentHash"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `playlists`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `playlist_tracks`"))
    }
}
