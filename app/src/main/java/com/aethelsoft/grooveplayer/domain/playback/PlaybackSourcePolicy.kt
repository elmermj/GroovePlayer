package com.aethelsoft.grooveplayer.domain.playback

/**
 * Elmer playback policy (SCRUM-62).
 *
 * 1. Local file (or a whole-object cache of it) always wins. Cloud is not consulted.
 * 2. Otherwise, if the song is still in the device catalog and cloud audio exists,
 *    stream only when [canStreamCloud] (Premium — same gate as availability badges).
 * 3. Otherwise, if it is in the catalog and cloud audio is confirmed missing, purge.
 *    Unknown cloud (offline, auth, route error) does not purge.
 */
enum class CloudAudioPresence {
    PRESENT,
    ABSENT,
    UNKNOWN,
}

enum class PlaybackDecision {
    PLAY_LOCAL,
    STREAM_CLOUD,
    PURGE,
    SKIP,
}

fun playbackDecision(
    localAvailable: Boolean,
    inCatalog: Boolean,
    cloud: CloudAudioPresence,
    canStreamCloud: Boolean,
): PlaybackDecision {
    if (localAvailable) return PlaybackDecision.PLAY_LOCAL
    if (!inCatalog) return PlaybackDecision.SKIP
    return when (cloud) {
        CloudAudioPresence.PRESENT ->
            if (canStreamCloud) PlaybackDecision.STREAM_CLOUD else PlaybackDecision.SKIP
        CloudAudioPresence.ABSENT -> PlaybackDecision.PURGE
        CloudAudioPresence.UNKNOWN -> PlaybackDecision.SKIP
    }
}

/**
 * Maps a resolved queue back onto the caller's start index.
 * The tapped song stays the start when it survived. Otherwise the next surviving
 * song after it plays; if none remain after it, playback starts at the first survivor.
 */
fun adjustedQueueStartIndex(
    originalIds: List<String>,
    startIndex: Int,
    playableIds: List<String>,
): Int {
    if (playableIds.isEmpty() || originalIds.isEmpty()) return 0
    val start = startIndex.coerceIn(0, originalIds.lastIndex)
    val tapped = originalIds[start]
    val direct = playableIds.indexOf(tapped)
    if (direct >= 0) return direct
    for (i in (start + 1) until originalIds.size) {
        val idx = playableIds.indexOf(originalIds[i])
        if (idx >= 0) return idx
    }
    return 0
}

/** Signed R2 / S3 GET URLs must not be handed to ExoPlayer (Range GET spam). */
fun looksLikeSignedObjectUrl(url: String): Boolean {
    val lower = url.lowercase()
    return "r2.cloudflarestorage" in lower ||
        "x-amz-algorithm=" in lower ||
        "x-amz-signature=" in lower
}
