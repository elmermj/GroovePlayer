package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.playback.adjustedQueueStartIndex

/**
 * Play starts on the chosen row. A missing row returns -1 so the caller leaves
 * the current queue alone instead of starting a different track.
 * A null tap is the playlist Play button: the first playable row, if any.
 */
fun playlistQueueStart(
    ids: List<String>,
    tappedIndex: Int?,
    playableIds: List<String>,
): Int {
    if (tappedIndex == null) return if (playableIds.isEmpty()) -1 else 0
    return adjustedQueueStartIndex(ids, tappedIndex, playableIds)
}
