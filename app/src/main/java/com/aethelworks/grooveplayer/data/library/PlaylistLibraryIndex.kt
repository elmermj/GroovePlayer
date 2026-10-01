package com.aethelworks.grooveplayer.data.library

import com.aethelworks.grooveplayer.data.local.db.dao.SongDao
import com.aethelworks.grooveplayer.domain.backup.ContentHash
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.playlist.PlaylistLibrary
import com.aethelworks.grooveplayer.domain.playlist.PlaylistLibrarySong
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Library songs plus the content hash SCRUM-84 stores on the song row. */
@Singleton
class PlaylistLibraryIndex @Inject constructor(
    private val catalog: PrivateLibraryCatalog,
    private val songDao: SongDao,
) : PlaylistLibrary {
    override suspend fun copies(): List<PlaylistLibrarySong> {
        return catalog.songs().mapNotNull { song ->
            val hash = hashFor(song) ?: return@mapNotNull null
            val name = song.filePath?.let { File(it).name }.orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            PlaylistLibrarySong(song = song, contentHash = hash, fileName = name)
        }
    }

    suspend fun hashFor(song: Song): String? {
        val path = song.filePath?.takeIf { it.isNotBlank() }
        val row = songDao.getSong(song.id) ?: path?.let { songDao.findBySourcePath(it) }
        val stored = row?.contentHash?.takeIf { it.isNotBlank() }
        if (stored != null) return stored.lowercase()
        val file = path?.let { File(it) } ?: return null
        if (!file.isFile) return null
        val computed = ContentHash.sha256(file).lowercase()
        if (row != null) songDao.updateContentHash(row.songId, computed)
        return computed
    }
}
