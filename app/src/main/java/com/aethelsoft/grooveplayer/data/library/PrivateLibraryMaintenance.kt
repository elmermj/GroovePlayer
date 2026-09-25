package com.aethelsoft.grooveplayer.data.library

import com.aethelsoft.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.backup.ContentHash
import com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySchemaMigration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Marks shared-folder catalog rows hidden and fills missing SHA-256 values.
 * Hashing only reads bytes. Original files are not moved or deleted.
 * A later launch continues from rows that still have no hash.
 */
@Singleton
class PrivateLibraryMaintenance @Inject constructor(
    private val songDao: SongDao,
    private val grooveDownloads: GrooveDownloadsLocator,
) {
    suspend fun markVisibility() = withContext(Dispatchers.IO) {
        val root = grooveDownloads.directory().absolutePath
        for (song in songDao.getAll()) {
            val visible = PrivateLibrarySchemaMigration.inPrivateLibrary(song.sourcePath, root)
            if (song.inPrivateLibrary != visible) {
                songDao.setPrivateFlag(song.songId, visible)
            }
        }
    }

    suspend fun backfillHashes(isActive: () -> Boolean = { true }) = withContext(Dispatchers.IO) {
        val pending = songDao.songsMissingHash()
        for (song in pending) {
            if (!isActive()) return@withContext
            val path = song.sourcePath ?: continue
            val file = File(path)
            if (!file.isFile || file.length() <= 0L || !file.canRead()) continue
            val hash = runCatching { ContentHash.sha256(file) }.getOrNull() ?: continue
            songDao.updateContentHash(song.songId, hash)
        }
    }
}
