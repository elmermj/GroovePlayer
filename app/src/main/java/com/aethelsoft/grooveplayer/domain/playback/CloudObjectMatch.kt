package com.aethelsoft.grooveplayer.domain.playback

import java.io.File

/**
 * Match a catalog song to a backup object the way backup upload does:
 * basename of logical_path, and size when both sides know it.
 */
fun cloudObjectMatchesSong(
    songPath: String?,
    songSizeBytes: Long?,
    objectLogicalPath: String?,
    objectSizeBytes: Long,
): Boolean {
    if (songPath.isNullOrBlank() || objectLogicalPath.isNullOrBlank()) return false
    val songName = File(songPath).name
    if (songName.isBlank()) return false
    val samePath = objectLogicalPath.equals(songPath, ignoreCase = true)
    val sameName = File(objectLogicalPath).name.equals(songName, ignoreCase = true)
    if (!samePath && !sameName) return false
    if (songSizeBytes != null && songSizeBytes > 0L && objectSizeBytes > 0L &&
        songSizeBytes != objectSizeBytes
    ) {
        return false
    }
    return true
}
