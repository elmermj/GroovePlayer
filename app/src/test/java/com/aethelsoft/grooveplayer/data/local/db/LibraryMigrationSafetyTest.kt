package com.aethelsoft.grooveplayer.data.local.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LibraryMigrationSafetyTest {

    @Test
    fun migrationSqlDoesNotDropOrDeleteUserTables() {
        val sql = GroovePlayerMigrations.auditedSql().joinToString("\n")
        assertFalse(Regex("\\bDROP\\b", RegexOption.IGNORE_CASE).containsMatchIn(sql))
        assertFalse(Regex("\\bDELETE\\s+FROM\\b", RegexOption.IGNORE_CASE).containsMatchIn(sql))
        assertTrue(sql.contains("playback_history"))
        assertTrue(sql.contains("song_likes"))
        assertTrue(sql.contains("playlists"))
        assertTrue(sql.contains("albums_preserved"))
    }

    @Test
    fun migrationsCoverEveryVersionThrough19WithoutGaps() {
        val steps = GroovePlayerMigrations.ALL
        assertEquals((1 until 19).toSet(), steps.map { it.startVersion }.toSet())
        steps.forEach { step ->
            assertEquals(step.startVersion + 1, step.endVersion)
        }
        assertEquals(19, steps.maxOf { it.endVersion })
        assertEquals(1, GroovePlayerMigrations.OLDEST_VERSION)
    }

    @Test
    fun openerDoesNotUseDestructiveFallback() {
        val source = openerSource()
        assertFalse(source.contains("fallbackToDestructive"))
        assertTrue(source.contains("addMigrations"))
        assertTrue(source.contains("needsAppUpdate"))
    }

    @Test
    fun downgradeKeepsTheDatabaseFile() {
        assertTrue(DatabaseOpenPolicy.isDowngrade(20, 19))
        assertFalse(DatabaseOpenPolicy.isDowngrade(19, 19))
        assertFalse(DatabaseOpenPolicy.isDowngrade(4, 19))
        assertFalse(DatabaseOpenPolicy.isDowngrade(null, 19))
        assertTrue(
            DatabaseOpenPolicy.keepFileOnOpenFailure(
                IllegalStateException(
                    "A migration from 16 to 15 was required but not found. Cannot downgrade database from version 20 to 19",
                ),
            ),
        )
        assertFalse(
            DatabaseOpenPolicy.keepFileOnOpenFailure(
                RuntimeException("database disk image is malformed"),
            ),
        )
    }

    @Test
    fun userVersionIsReadFromTheSqliteHeader() {
        val file = File.createTempFile("library", ".db")
        file.writeBytes(ByteArray(64))
        assertNull(SqliteUserVersion.read(file))
        val header = ByteArray(64)
        "SQLite format 3\u0000".encodeToByteArray().copyInto(header)
        header[60] = 0
        header[61] = 0
        header[62] = 0
        header[63] = 4
        file.writeBytes(header)
        assertEquals(4, SqliteUserVersion.read(file))
        assertTrue(DatabaseOpenPolicy.isDowngrade(SqliteUserVersion.read(file), 3))
        file.delete()
    }

    private fun openerSource(): String {
        var dir = File(System.getProperty("user.dir") ?: error("user.dir"))
        repeat(6) {
            val candidates = listOf(
                File(dir, "src/main/java/com/aethelsoft/grooveplayer/data/local/db/LibraryDatabaseOpener.kt"),
                File(dir, "app/src/main/java/com/aethelsoft/grooveplayer/data/local/db/LibraryDatabaseOpener.kt"),
            )
            candidates.firstOrNull { it.isFile }?.let { return it.readText() }
            dir = dir.parentFile ?: return@repeat
        }
        error("LibraryDatabaseOpener.kt not found from ${System.getProperty("user.dir")}")
    }
}
