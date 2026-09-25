package com.aethelsoft.grooveplayer.data.repository

import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.backup.ContentHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Refreshes songs.contentHash after a tag edit using the existing hash API. */
@Singleton
class EditedSongContentHash @Inject constructor(
    private val songDao: SongDao,
) {
    suspend fun refresh(songId: String, file: File) = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext
        val hash = ContentHash.sha256(file)
        if (songId.isNotBlank()) {
            songDao.updateContentHash(songId, hash)
        }
        val row = songDao.findBySourcePath(file.absolutePath)
        if (row != null && row.songId != songId) {
            songDao.updateContentHash(row.songId, hash)
        }
    }
}
