package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier

/**
 * Elmer playback policy (SCRUM-62). Cloud streaming is active-premium only.
 *
 * 1. Local file (or a whole-object cache of it) always plays, for every tier.
 *    Playback routes are not called.
 * 2. Local missing + catalog row + cloud object **exists** → stream only when the
 *    server says `entitled` (active premium). Free, basic, and premium with
 *    `grace_until` still in the future are not entitled: skip and keep the row.
 * 3. Local missing + **404 `{exists:false}`** → purge, for every tier.
 *    That is the only purge signal. Network, 401, and 5xx are not "absent".
 *
 * Purge means the object is gone. The premium gate is entitlement to stream
 * when the object exists (including an open grace window).
 */
enum class CloudAudioPresence {
    PRESENT,
    ABSENT,
    UNKNOWN,
}

enum class PlaybackDecision {
    PLAY_LOCAL,
    STREAM_CLOUD,
    /** Cloud object confirmed absent (404 exists:false). */
    PURGE,
    /** Do not play. Catalog stays. Includes "exists, not entitled". */
    SKIP,
}

enum class PlaybackDropReason {
    /** Cloud object confirmed absent. Catalog row is deleted. */
    PURGED,
    /** Cloud object exists; user is not entitled to stream. Catalog stays. */
    NOT_ENTITLED,
    /** Not in the catalog, cloud unknown, or the stream URL is not playable. Catalog stays. */
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
        // Object exists. Active premium may stream it. Anyone else skips; the row stays.
        CloudAudioPresence.PRESENT ->
            if (canStreamCloud) PlaybackDecision.STREAM_CLOUD else PlaybackDecision.SKIP
        // 404 exists:false. Delete the catalog row for every tier.
        CloudAudioPresence.ABSENT -> PlaybackDecision.PURGE
        // Not proven absent. Do not purge and do not stream.
        CloudAudioPresence.UNKNOWN -> PlaybackDecision.SKIP
    }
}

/**
 * Same rule the server uses for `entitled`. Tier premium is not enough:
 * an open grace window (`grace_until` still in the future) cannot stream.
 * Null [graceUntilEpochMs] means the window is closed.
 */
fun activeCloudStreamEntitled(
    tier: PrivilegeTier,
    graceUntilEpochMs: Long?,
    nowEpochMs: Long,
): Boolean {
    if (tier != PrivilegeTier.PREMIUM) return false
    val graceUntil = graceUntilEpochMs ?: return true
    return graceUntil <= nowEpochMs
}

/**
 * GET `/v1/playback/objects`.
 * 404 or `exists: false` is absence. 401 and 5xx are unknown. Entitlement is separate.
 */
fun interpretPlaybackObjects(
    httpCode: Int,
    exists: Boolean?,
    entitled: Boolean?,
): Pair<CloudAudioPresence, Boolean> {
    if (httpCode == 401 || httpCode in 500..599) {
        return CloudAudioPresence.UNKNOWN to false
    }
    if (httpCode == 404 || exists == false) {
        return CloudAudioPresence.ABSENT to false
    }
    if (httpCode == 200 && exists == true) {
        return CloudAudioPresence.PRESENT to (entitled == true)
    }
    if (httpCode == 403 && exists == true) {
        return CloudAudioPresence.PRESENT to false
    }
    return CloudAudioPresence.UNKNOWN to false
}

/**
 * POST `/v1/playback/stream-url`.
 * 403 keeps the row. 404 purges. dry_run is not playable and is not absence.
 * 401 / 5xx leave the row.
 */
fun interpretStreamUrl(
    httpCode: Int,
    exists: Boolean?,
    entitled: Boolean,
    dryRun: Boolean,
    streamUrl: String?,
): CloudStreamOpen {
    if (httpCode == 401 || httpCode in 500..599) return CloudStreamOpen.Unknown
    if (httpCode == 404 || exists == false) return CloudStreamOpen.Absent
    if (httpCode == 403 || !entitled) return CloudStreamOpen.NotEntitled
    if (dryRun || isDryRunPlaybackUrl(streamUrl)) return CloudStreamOpen.Unusable
    val url = streamUrl?.takeIf { it.isNotBlank() } ?: return CloudStreamOpen.Unknown
    return CloudStreamOpen.Play(url)
}

/** Placeholder host until R2 credentials exist. Not real audio. */
fun isDryRunPlaybackUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return "dry-run.r2.local" in url.lowercase()
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

/**
 * Signed R2 / S3 GET URLs. Playback may hand `stream_url` to ExoPlayer (Range is
 * allowed for seek). Backup restore must still download the whole object and
 * must not send Range.
 */
fun looksLikeSignedObjectUrl(url: String): Boolean {
    val lower = url.lowercase()
    return "r2.cloudflarestorage" in lower ||
        "x-amz-algorithm=" in lower ||
        "x-amz-signature=" in lower
}
