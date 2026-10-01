package com.aethelworks.grooveplayer.data.library

import com.aethelworks.grooveplayer.data.playback.CloudSongCatalog
import com.aethelworks.grooveplayer.domain.backup.AppLibraryPaths
import com.aethelworks.grooveplayer.domain.library.TrackPresence
import com.aethelworks.grooveplayer.domain.library.trackPresence
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.domain.playback.SongCatalog
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
