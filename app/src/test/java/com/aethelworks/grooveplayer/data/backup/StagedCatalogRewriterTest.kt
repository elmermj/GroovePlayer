package com.aethelworks.grooveplayer.data.backup

import com.aethelworks.grooveplayer.domain.backup.PlacedCloudSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class StagedCatalogRewriterTest {

    @Test
    fun stagedDatabaseWithoutSourcePathDoesNotThrow() {
        withSongs(includeSourcePath = false) { db, connection ->
            StagedCatalogRewriter.rewrite(db, placements())
            assertTrue(columnNames(connection).any { it.equals("sourcePath", ignoreCase = true) })
            assertNull(sourcePath(connection, "s1"))
        }
    }

    @Test
    fun existingSourcePathIsRewrittenToThePlacedFile() {
        withSongs(includeSourcePath = true) { db, connection ->
            StagedCatalogRewriter.rewrite(db, placements())
            assertEquals("/data/library/abc.mp3", sourcePath(connection, "s1"))
        }
    }

    @Test
    fun missingSongsTableDoesNotThrow() {
        val file = tempDatabase()
        try {
            connection(file).use { connection ->
                StagedCatalogRewriter.rewrite(JdbcCatalogDb(connection), placements())
            }
        } finally {
            deleteDatabase(file)
        }
    }

    private fun placements() = listOf(
        PlacedCloudSong(
            contentHash = "abc",
            sizeBytes = 4,
            logicalPath = "/sdcard/Music/track.mp3",
            localPath = "/data/library/abc.mp3",
        ),
    )

    private fun withSongs(
        includeSourcePath: Boolean,
        block: (StagedCatalogRewriter.CatalogDb, Connection) -> Unit,
    ) {
        val file = tempDatabase()
        try {
            connection(file).use { connection ->
                connection.createStatement().use { statement ->
                    val sourcePathColumn = if (includeSourcePath) ", sourcePath TEXT" else ""
                    statement.execute(
                        """
                        CREATE TABLE songs (
                            songId TEXT NOT NULL PRIMARY KEY,
                            uri TEXT NOT NULL,
                            title TEXT NOT NULL
                            $sourcePathColumn
                        )
                        """.trimIndent(),
                    )
                    if (includeSourcePath) {
                        statement.execute(
                            "INSERT INTO songs (songId, uri, title, sourcePath) VALUES " +
                                "('s1', 'content://media/1', 'Track', '/sdcard/Music/track.mp3')",
                        )
                    } else {
                        statement.execute(
                            "INSERT INTO songs (songId, uri, title) VALUES " +
                                "('s1', 'content://media/1', 'Old')",
                        )
                    }
                }
                block(JdbcCatalogDb(connection), connection)
            }
        } finally {
            deleteDatabase(file)
        }
    }

    private fun columnNames(connection: Connection): List<String> {
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(songs)").use { result ->
                val names = mutableListOf<String>()
                while (result.next()) names += result.getString("name")
                return names
            }
        }
    }

    private fun sourcePath(connection: Connection, songId: String): String? {
        connection.prepareStatement("SELECT sourcePath FROM songs WHERE songId = ?").use { statement ->
            statement.setString(1, songId)
            statement.executeQuery().use { result ->
                if (!result.next()) return null
                val value = result.getString(1)
                return if (result.wasNull()) null else value
            }
        }
    }

    private fun tempDatabase(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "groove-rewrite-${System.nanoTime()}")
        dir.mkdirs()
        return File(dir, "staged.db")
    }

    private fun connection(file: File): Connection {
        Class.forName("org.sqlite.JDBC")
        return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
    }

    private fun deleteDatabase(file: File) {
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
        file.parentFile?.delete()
    }
}

private class JdbcCatalogDb(
    private val connection: Connection,
) : StagedCatalogRewriter.CatalogDb {
    override fun query(sql: String, args: Array<String>?): List<Map<String, String?>> {
        connection.prepareStatement(sql).use { statement ->
            args?.forEachIndexed { index, arg -> statement.setString(index + 1, arg) }
            statement.executeQuery().use { result ->
                val meta = result.metaData
                val rows = mutableListOf<Map<String, String?>>()
                while (result.next()) {
                    val row = LinkedHashMap<String, String?>(meta.columnCount)
                    for (index in 1..meta.columnCount) {
                        val value = result.getString(index)
                        row[meta.getColumnLabel(index)] = if (result.wasNull()) null else value
                    }
                    rows += row
                }
                return rows
            }
        }
    }

    override fun exec(sql: String, args: Array<Any?>) {
        connection.prepareStatement(sql).use { statement ->
            args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
            statement.execute()
        }
    }

    override fun transaction(block: () -> Unit) {
        val previous = connection.autoCommit
        connection.autoCommit = false
        try {
            block()
            connection.commit()
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previous
        }
    }
}
