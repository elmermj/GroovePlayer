package com.aethelworks.grooveplayer.data.player

import com.aethelworks.grooveplayer.domain.model.RepeatMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decisions for an overlapping crossfade between two players.
 *
 * The main player becomes the incoming track so the session, equalizer, and
 * notification follow the song that is fading in. A second player, without
 * audio focus, keeps the outgoing track audible. Repeat-one loops the same
 * item and does not crossfade into itself.
 */
internal object TrackCrossfade {
    /** Prepare the outgoing player this long before the fade window. */
    const val PRELOAD_LEAD_MS = 1_500L

    /** Shorter overlaps are a hard cut. */
    const val MIN_FADE_MS = 400L

    /** Give the outgoing player this long to reach the handoff position. */
    const val ARM_TIMEOUT_MS = 1_500L

    enum class Phase { IDLE, PRELOADING, ARMING, OVERLAPPING }

    data class NaturalInput(
        val fadeDurationMs: Long,
        val positionMs: Long,
        val durationMs: Long,
        val playWhenReady: Boolean,
        val heldAtEnd: Boolean,
        val currentIndex: Int,
        val itemCount: Int,
        val repeatMode: RepeatMode,
        val phase: Phase,
        val outgoingReady: Boolean,
    )

    sealed interface Step {
        data object None : Step
        data object PreloadOutgoing : Step
        data class Begin(val nextIndex: Int, val fadeMs: Long) : Step
        data class Volumes(val outgoing: Float, val incoming: Float) : Step
        data object Finish : Step
        data object CancelOutgoing : Step
        data class HardCut(val nextIndex: Int) : Step
    }

    /**
     * Fade length that fits inside [durationMs]. Unknown or tiny tracks return 0
     * so playback is left to ExoPlayer instead of pausing at the end.
     */
    fun effectiveFadeMs(requestedMs: Long, durationMs: Long): Long {
        if (requestedMs <= 0L || durationMs <= 0L) return 0L
        val capped = minOf(requestedMs, (durationMs - 200L).coerceAtLeast(0L))
        return if (capped >= MIN_FADE_MS) capped else 0L
    }

    /** Fade for a skip. Uses the audio still left in the outgoing track. */
    fun manualFadeMs(requestedMs: Long, remainingMs: Long): Long {
        if (requestedMs < MIN_FADE_MS || remainingMs < MIN_FADE_MS) return 0L
        return minOf(requestedMs, remainingMs)
    }

    /**
     * Next item for an automatic transition. Repeat-one stays on the current
     * item, which is a loop rather than a crossfade into another track.
     */
    fun autoNextIndex(currentIndex: Int, itemCount: Int, repeatMode: RepeatMode): Int? {
        if (itemCount <= 0 || currentIndex !in 0 until itemCount) return null
        if (repeatMode == RepeatMode.ONE) return null
        val next = currentIndex + 1
        return when {
            next < itemCount -> next
            repeatMode == RepeatMode.ALL && itemCount > 1 -> 0
            else -> null
        }
    }

    /** Next item for the skip button. Repeat-one advances and does not wrap. */
    fun skipNextIndex(currentIndex: Int, itemCount: Int, repeatMode: RepeatMode): Int? {
        if (itemCount <= 0 || currentIndex !in 0 until itemCount) return null
        val next = currentIndex + 1
        return when {
            next < itemCount -> next
            repeatMode == RepeatMode.ALL && itemCount > 1 -> 0
            else -> null
        }
    }

    fun skipPreviousIndex(currentIndex: Int, itemCount: Int, repeatMode: RepeatMode): Int? {
        if (itemCount <= 0 || currentIndex !in 0 until itemCount) return null
        return when {
            currentIndex > 0 -> currentIndex - 1
            repeatMode == RepeatMode.ALL && itemCount > 1 -> itemCount - 1
            else -> null
        }
    }

    fun shouldPauseAtEnd(
        fadeSeconds: Int,
        durationMs: Long,
        currentIndex: Int,
        itemCount: Int,
        repeatMode: RepeatMode,
    ): Boolean {
        if (effectiveFadeMs(fadeSeconds * 1000L, durationMs) <= 0L) return false
        return autoNextIndex(currentIndex, itemCount, repeatMode) != null
    }

    /** Equal-power pair. At the midpoint both sides are about 0.707 so loudness stays flat. */
    fun equalPowerGains(elapsedMs: Long, fadeMs: Long, audible: Float): Pair<Float, Float> {
        if (fadeMs <= 0L) return audible to 0f
        val progress = (elapsedMs.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
        val angle = progress * (PI.toFloat() / 2f)
        return (cos(angle) * audible) to (sin(angle) * audible)
    }

    fun planNatural(input: NaturalInput): Step {
        if (input.phase == Phase.ARMING || input.phase == Phase.OVERLAPPING) return Step.None
        val next = autoNextIndex(input.currentIndex, input.itemCount, input.repeatMode)
        val fade = effectiveFadeMs(input.fadeDurationMs, input.durationMs)
        if (fade == 0L || next == null) {
            return if (input.phase == Phase.PRELOADING) Step.CancelOutgoing else Step.None
        }
        if (input.heldAtEnd) return Step.HardCut(next)
        if (!input.playWhenReady) return Step.None

        val remaining = input.durationMs - input.positionMs
        if (remaining > fade + PRELOAD_LEAD_MS) {
            return if (input.phase == Phase.PRELOADING) Step.CancelOutgoing else Step.None
        }
        if (remaining > fade) {
            return if (input.phase == Phase.IDLE) Step.PreloadOutgoing else Step.None
        }
        if (input.outgoingReady) {
            val overlap = minOf(fade, remaining.coerceAtLeast(0L))
            return if (overlap >= MIN_FADE_MS) Step.Begin(next, overlap) else Step.HardCut(next)
        }
        if (remaining <= 200L || (input.phase == Phase.IDLE && remaining <= MIN_FADE_MS)) {
            return Step.HardCut(next)
        }
        return if (input.phase == Phase.IDLE) Step.PreloadOutgoing else Step.None
    }

    fun planOverlap(
        incomingPositionMs: Long,
        stallMs: Long,
        fadeDurationMs: Long,
        audible: Float,
        playWhenReady: Boolean,
        incomingPlaying: Boolean,
    ): Step {
        if (!playWhenReady) return Step.None
        if (fadeDurationMs <= 0L) return Step.Finish
        if (!incomingPlaying) {
            return if (stallMs >= ARM_TIMEOUT_MS) {
                Step.Finish
            } else {
                Step.Volumes(outgoing = audible, incoming = 0f)
            }
        }
        if (incomingPositionMs >= fadeDurationMs) return Step.Finish
        val (outgoing, incoming) = equalPowerGains(incomingPositionMs, fadeDurationMs, audible)
        return Step.Volumes(outgoing, incoming)
    }
}
