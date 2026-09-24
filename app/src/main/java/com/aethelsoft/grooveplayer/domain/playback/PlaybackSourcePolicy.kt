package com.aethelsoft.grooveplayer.domain.playback

/**
 * Elmer playback policy (SCRUM-62). Cloud streaming is Premium-only.
 *
 * 1. Local file (or a whole-object cache of it) always plays, for every tier. Cloud is not consulted.
 * 2. Local missing + catalog row + cloud object **exists** → stream only when [canStreamCloud]
 *    (PrivilegeTier.PREMIUM, same check as backup and availability badges).
 *    Not entitled: skip playback and keep the catalog row. That is not a purge.
 * 3. Local missing + cloud object **confirmed absent** → purge, for every tier.
 *    Unknown cloud (offline, auth, route error) is not "absent" and does not purge.
 *
 * Purge means the object is gone. The Premium gate only decides whether an existing object may be streamed.
 */
enum class CloudAudioPresence {
    PRESENT,
    ABSENT,
    UNKNOWN,
}

enum class PlaybackDecision {
    PLAY_LOCAL,
    STREAM_CLOUD,
    /** Cloud object confirmed absent. */
    PURGE,
    /** Do not play. Catalog stays. Includes "cloud exists, not Premium". */
    SKIP,
}

enum class PlaybackDropReason {
    /** Cloud object confirmed absent. Catalog row is deleted. */
    PURGED,
    /** Cloud object exists; user is not Premium. Skip and offer upgrade. Catalog stays. */
    NOT_ENTITLED,
    /** Not in the catalog, cloud unknown, or the download failed. Catalog stays. */
    UNAVAILABLE,
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
        // Object exists. Premium may stream it. Anyone else skips; the row stays.
        CloudAudioPresence.PRESENT ->
            if (canStreamCloud) PlaybackDecision.STREAM_CLOUD else PlaybackDecision.SKIP
        // Object truly gone. Delete the catalog row for every tier.
        CloudAudioPresence.ABSENT -> PlaybackDecision.PURGE
        // Not proven absent. Do not purge and do not stream.
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
