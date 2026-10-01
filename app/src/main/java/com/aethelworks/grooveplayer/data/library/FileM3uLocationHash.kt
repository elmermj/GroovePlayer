package com.aethelworks.grooveplayer.data.library

import com.aethelworks.grooveplayer.domain.backup.ContentHash
import com.aethelworks.grooveplayer.domain.playlist.M3uLocationHash
import com.aethelworks.grooveplayer.domain.playlist.PlaylistM3uMatch
import javax.inject.Inject
import javax.inject.Singleton

/** Hashes a readable M3U path so import can match [songs.contentHash]. Does not play the file. */
@Singleton
class FileM3uLocationHash @Inject constructor() : M3uLocationHash {
    override fun sha256OrNull(location: String): String? {
        val file = PlaylistM3uMatch.readableFile(location) ?: return null
        return runCatching { ContentHash.sha256(file).lowercase() }.getOrNull()
    }
}
