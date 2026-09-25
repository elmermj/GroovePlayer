package com.aethelsoft.grooveplayer.data.library

import com.aethelsoft.grooveplayer.data.playback.CloudSongCatalog
import com.aethelsoft.grooveplayer.domain.backup.AppLibraryPaths
import com.aethelsoft.grooveplayer.domain.library.TrackPresence
import com.aethelsoft.grooveplayer.domain.library.trackPresence
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.SongCatalog
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A song is playable only when its private-library file is on disk.
 * History, likes, and queue rows keep their stable id and stay visible when the file is gone.
 */
@Singleton
class TrackPresenceRepository @Inject constructor(
    private val appLibrary: AppLibraryPaths,
    private val catalog: SongCatalog,
    private val cloud: CloudSongCatalog,
) {
    suspend fun presence(song: Song): TrackPresence {
        val path = song.filePath?.takeIf { appLibrary.isInside(it) }
            ?: catalog.sourcePath(song.id)?.takeIf { appLibrary.isInside(it) }
        val file = path?.let { File(it) }
        val local = file != null && file.isFile && file.length() > 0L
        if (local) return trackPresence(localFilePresent = true, cloudCopyExists = false)
        val cloudCopy = runCatching {
            cloud.isBackedUp(song.id, path ?: song.filePath, song.fileSizeBytes)
        }.getOrDefault(false)
        return trackPresence(localFilePresent = false, cloudCopyExists = cloudCopy)
    }
}
