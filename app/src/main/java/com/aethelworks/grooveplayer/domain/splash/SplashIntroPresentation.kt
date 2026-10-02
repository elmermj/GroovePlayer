package com.aethelworks.grooveplayer.domain.splash

/**
 * Cold-start splash playback.
 *
 * The intro (frames 0–90, about 1.5s at 60fps) always plays once. It is never
 * skipped, even when the first screen is already composed. After that, frames
 * 90–234 loop only while startup has not released. A failed composition cannot
 * play those frames; the static logo stays up for the same minimum, then until
 * the app is ready.
 */
object SplashIntroPresentation {

    const val INTRO_MIN_FRAME = 0
    const val INTRO_MAX_FRAME = 90
    const val LOOP_MIN_FRAME = 90
    const val LOOP_MAX_FRAME = 234

    /** 90 frames at 60fps. The intro is not dismissed before this. */
    const val INTRO_MIN_VISIBLE_MS = 1_500L

    enum class Phase {
        INTRO,
        LOOP,
        DONE,
    }

    data class FrameClip(val minFrame: Int, val maxFrame: Int)

    fun phase(
        introFinished: Boolean,
        elapsedMs: Long,
        appReady: Boolean,
        compositionFailed: Boolean = false,
    ): Phase {
        if (compositionFailed) {
            return if (appReady && elapsedMs >= INTRO_MIN_VISIBLE_MS) Phase.DONE else Phase.INTRO
        }
        if (!introFinished || elapsedMs < INTRO_MIN_VISIBLE_MS) return Phase.INTRO
        if (!appReady) return Phase.LOOP
        return Phase.DONE
    }

    fun clipFor(phase: Phase): FrameClip = when (phase) {
        Phase.LOOP -> FrameClip(LOOP_MIN_FRAME, LOOP_MAX_FRAME)
        Phase.INTRO, Phase.DONE -> FrameClip(INTRO_MIN_FRAME, INTRO_MAX_FRAME)
    }

    /** Matches [com.airbnb.lottie.compose.LottieConstants.IterateForever] for the loop. */
    fun iterations(phase: Phase): Int = when (phase) {
        Phase.LOOP -> Int.MAX_VALUE
        Phase.INTRO, Phase.DONE -> 1
    }

    fun clipDescription(phase: Phase): String {
        val clip = clipFor(phase)
        val name = if (phase == Phase.LOOP) "loop" else "intro"
        return "LottieCompositionSpec.RawRes splash_intro clipSpec $name " +
            "minFrame=${clip.minFrame} maxFrame=${clip.maxFrame}"
    }

    fun clipPlan(): String =
        "clipSpec intro minFrame=$INTRO_MIN_FRAME maxFrame=$INTRO_MAX_FRAME " +
            "then loop minFrame=$LOOP_MIN_FRAME maxFrame=$LOOP_MAX_FRAME"
}
