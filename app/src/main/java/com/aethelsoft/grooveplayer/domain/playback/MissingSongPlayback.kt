package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.Song

/** Index of [startSongId] in [songs], or null when that song is not in the list. */
fun continueListeningIndex(songs: List<Song>, startSongId: String): Int? {
    val index = songs.indexOfFirst { it.id == startSongId }
    return index.takeIf { it >= 0 }
}

data class RestoredPlayback(
    val songs: List<Song>,
    val startIndex: Int,
)

/**
 * Rebuild a saved queue by song id. If the song that was playing is not available,
 * return null so the caller leaves the current queue alone.
 */
fun restorePlaybackById(
    savedIds: List<String>,
    savedStartIndex: Int,
    available: List<Song>,
): RestoredPlayback? {
    if (savedIds.isEmpty()) return null
    val byId = available.associateBy { it.id }
    val startId = savedIds.getOrNull(savedStartIndex) ?: return null
    if (byId[startId] == null) return null
    val songs = savedIds.mapNotNull { byId[it] }
    val start = songs.indexOfFirst { it.id == startId }
    if (start < 0) return null
    return RestoredPlayback(songs, start)
}
