package com.aethelsoft.grooveplayer.domain.playback

import com.aethelsoft.grooveplayer.domain.model.RepeatMode

/** Preferred on-disk ExoPlayer cache for premium cloud streams. */
const val STREAM_CACHE_PREFERRED_BYTES: Long = 256L * 1024 * 1024

/** Below this, prefer a smaller cap instead of the preferred size. */
const val STREAM_CACHE_FLOOR_BYTES: Long = 32L * 1024 * 1024

/** Leave this much free space before taking the preferred cache. */
const val STREAM_DISK_HEADROOM_BYTES: Long = 512L * 1024 * 1024

/** On a tighter volume, still leave this much free. */
const val STREAM_CACHE_TIGHT_HEADROOM_BYTES: Long = 256L * 1024 * 1024

/** Hard cap when the volume cannot spare the preferred cache. */
const val STREAM_CACHE_TIGHT_CAP_BYTES: Long = 16L * 1024 * 1024

/**
 * How much of the next cloud track to pull ahead of playback.
 * About a minute of 256 kbps audio, not the whole file.
 */
const val STREAM_PREFETCH_MAX_BYTES: Long = 2L * 1024 * 1024

const val STREAM_REFRESH_FAILED_MESSAGE: String =
    "Couldn't refresh this stream. Try again."

/**
 * Bytes ExoPlayer may keep for cloud streams.
 * [usableBytes] is free space on the cache volume. Zero means play without a disk cache.
 */
fun streamCacheLimitBytes(usableBytes: Long): Long {
    if (usableBytes <= 0L) return 0L
    val room = usableBytes - STREAM_DISK_HEADROOM_BYTES
    if (room >= STREAM_CACHE_FLOOR_BYTES) {
        return room.coerceAtMost(STREAM_CACHE_PREFERRED_BYTES)
    }
    val tight = (usableBytes - STREAM_CACHE_TIGHT_HEADROOM_BYTES).coerceAtLeast(0L)
    return tight.coerceAtMost(STREAM_CACHE_TIGHT_CAP_BYTES)
}

/**
 * Cache key for a `groove-playback://` item. The signed URL is not part of the key,
 * so a refreshed URL still hits bytes already stored. A new content hash misses.
 */
fun streamCacheKey(uri: String, contentHash: String?): String {
    val hash = contentHash?.takeIf { it.isNotBlank() } ?: return uri
    return "$uri#$hash"
}

/**
 * URI of the next queue item when that item will stream from the cloud.
 * Local files are not prefetched. Repeat-one is already the current item.
 * Repeat-all wraps to the first item.
 */
fun nextCloudStreamUri(
    queueUris: List<String>,
    currentIndex: Int,
    repeat: RepeatMode,
): String? {
    if (queueUris.isEmpty() || currentIndex !in queueUris.indices) return null
    if (repeat == RepeatMode.ONE) return null
    val nextIndex = when {
        currentIndex + 1 <= queueUris.lastIndex -> currentIndex + 1
        repeat == RepeatMode.ALL && queueUris.size > 1 -> 0
        else -> return null
    }
    val uri = queueUris[nextIndex]
    if (!uri.startsWith("$PLAYBACK_STREAM_SCHEME://")) return null
    return uri
}

/**
 * What the player should do after a cloud-stream open or mid-play failure.
 * The queue and the signed-in session stay either way.
 */
enum class StreamRecoveryStep {
    /** POST stream-url again and resume at the same position. */
    REFRESH_URL,
    /** Cloud object is gone. Advance when another queue item exists. */
    SKIP_ABSENT,
    /** Not entitled. Premium prompt already fired. Do not start a stream. */
    HOLD_FOR_PREMIUM,
    /** A refresh was already tried and playback still failed. */
    SURFACE_FAILURE,
    /** Not a stream-URL failure (for example a decoder error). */
    IGNORE,
}

data class StreamPlaybackFault(
    val httpStatus: Int? = null,
    val detail: String = "",
    /** True for network and HTTP open failures. False for decoder or parser errors. */
    val ioFailure: Boolean = false,
)

fun streamRecoveryStep(
    fault: StreamPlaybackFault,
    alreadyRefreshed: Boolean,
): StreamRecoveryStep {
    val detail = fault.detail.lowercase()
    if ("object not found" in detail) return StreamRecoveryStep.SKIP_ABSENT
    if ("premium required" in detail) return StreamRecoveryStep.HOLD_FOR_PREMIUM
    val http = fault.httpStatus ?: responseCodeIn(detail)
    val refreshable = http == 401 || http == 403 || "expired" in detail || fault.ioFailure
    if (!refreshable) return StreamRecoveryStep.IGNORE
    return if (alreadyRefreshed) {
        StreamRecoveryStep.SURFACE_FAILURE
    } else {
        StreamRecoveryStep.REFRESH_URL
    }
}

private fun responseCodeIn(detail: String): Int? {
    val match = Regex("""response code:\s*(\d{3})""").find(detail) ?: return null
    return match.groupValues[1].toIntOrNull()
}
