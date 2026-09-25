package com.aethelsoft.grooveplayer.data.artwork

import com.aethelsoft.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.backup.AppPrivateLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio file for a `songs.contentHash`, using the private-library path already
 * stored on the song row. Does not hash file bytes.
 */
@Singleton
class SongArtworkFiles @Inject constructor(
    private val songDao: SongDao,
    private val library: GrooveDownloadsLocator,
) {
    suspend fun audioFile(contentHash: String): File? = withContext(Dispatchers.IO) {
        val root = library.directory().absolutePath
        songDao.findByContentHash(contentHash).firstNotNullOfOrNull { song ->
            val path = song.sourcePath ?: return@firstNotNullOfOrNull null
            if (!song.inPrivateLibrary || !AppPrivateLibrary.isInside(path, root)) {
                return@firstNotNullOfOrNull null
            }
            File(path).takeIf { it.isFile && it.length() > 0L && it.canRead() }
        }
    }
}
