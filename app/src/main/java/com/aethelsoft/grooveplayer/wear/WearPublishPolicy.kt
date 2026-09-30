package com.aethelsoft.grooveplayer.wear

import kotlin.math.abs

/**
 * What the phone last told the watch, plus the fields needed to decide whether
 * another DataItem is worth sending.
 */
data class WearPlaybackSample(
    val songId: String?,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long = 0L,
    val filePath: String? = null,
    val artworkUrl: String? = null,
)

object WearPublishPolicy {
    const val INTERVAL_MS = 5_000L
    const val SEEK_JUMP_MS = 1_500L

    /**
     * Publish on a track change, a play/pause change, a seek, and on a slow
     * tick while playing so the watch can correct its local interpolation.
     * Position ticks from the player (~300ms) do not each become a message.
     */
    fun shouldPublish(
        watchConnected: Boolean,
        previous: WearPlaybackSample?,
        current: WearPlaybackSample,
        nowMs: Long,
        lastSentAtMs: Long,
        intervalMs: Long = INTERVAL_MS,
    ): Boolean {
        if (!watchConnected) return false
        if (previous == null) return true
        if (previous.songId != current.songId) return true
        if (previous.isPlaying != current.isPlaying) return true
        val elapsed = (nowMs - lastSentAtMs).coerceAtLeast(0L)
        val expected = if (previous.isPlaying) previous.positionMs + elapsed else previous.positionMs
        if (abs(current.positionMs - expected) > SEEK_JUMP_MS) return true
        return current.isPlaying && elapsed >= intervalMs
    }
}
