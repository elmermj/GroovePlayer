package com.aethelsoft.grooveplayer.wear.logic

/**
 * Local progress while the phone says the track is playing.
 * [updatedAtMs] is the phone clock when [positionMs] was sampled.
 * A watch clock behind the phone does not rewind the playhead.
 */
fun interpolatedPositionMs(
    positionMs: Long,
    isPlaying: Boolean,
    updatedAtMs: Long,
    nowMs: Long,
    durationMs: Long,
): Long {
    if (durationMs <= 0L) return 0L
    val duration = durationMs
    if (!isPlaying) return positionMs.coerceIn(0L, duration)
    val elapsed = (nowMs - updatedAtMs).coerceAtLeast(0L)
    return (positionMs + elapsed).coerceIn(0L, duration)
}

fun progressFraction(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

fun positionForFraction(fraction: Float, durationMs: Long): Long {
    if (durationMs <= 0L) return 0L
    return (fraction.coerceIn(0f, 1f) * durationMs.toFloat()).toLong().coerceIn(0L, durationMs)
}

fun formatClock(positionMs: Long): String {
    val total = positionMs.coerceAtLeast(0L) / 1000L
    val seconds = (total % 60).toInt()
    val minutes = ((total / 60) % 60).toInt()
    val hours = (total / 3600).toInt()
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
