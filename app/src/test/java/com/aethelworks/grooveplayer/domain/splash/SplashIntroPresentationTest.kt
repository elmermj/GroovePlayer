package com.aethelworks.grooveplayer.domain.splash

import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.domain.splash.SplashIntroPresentation.Phase
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SplashIntroPresentationTest {

    @Test
    fun introPlaysFullyEvenWhenTheAppIsAlreadyReady() {
        assertEquals(
            Phase.INTRO,
            SplashIntroPresentation.phase(
                introFinished = false,
                elapsedMs = 5_000,
                appReady = true,
            ),
        )
        assertEquals(
            Phase.INTRO,
            SplashIntroPresentation.phase(
                introFinished = true,
                elapsedMs = SplashIntroPresentation.INTRO_MIN_VISIBLE_MS - 1,
                appReady = true,
            ),
        )
        assertEquals(
            Phase.DONE,
            SplashIntroPresentation.phase(
                introFinished = true,
                elapsedMs = SplashIntroPresentation.INTRO_MIN_VISIBLE_MS,
                appReady = true,
            ),
        )
    }

    @Test
    fun loopContinuesUntilTheAppIsReady() {
        assertEquals(
            Phase.LOOP,
            SplashIntroPresentation.phase(
                introFinished = true,
                elapsedMs = SplashIntroPresentation.INTRO_MIN_VISIBLE_MS,
                appReady = false,
            ),
        )
        assertEquals(
            Phase.LOOP,
            SplashIntroPresentation.phase(
                introFinished = true,
                elapsedMs = 8_000,
                appReady = false,
            ),
        )
        assertEquals(
            Phase.DONE,
            SplashIntroPresentation.phase(
                introFinished = true,
                elapsedMs = 8_000,
                appReady = true,
            ),
        )
    }

    @Test
    fun clipSpecIsIntroThenLoop() {
        val intro = SplashIntroPresentation.clipFor(Phase.INTRO)
        val loop = SplashIntroPresentation.clipFor(Phase.LOOP)
        assertEquals(SplashIntroPresentation.FrameClip(0, 90), intro)
        assertEquals(SplashIntroPresentation.FrameClip(90, 234), loop)
        assertEquals(1, SplashIntroPresentation.iterations(Phase.INTRO))
        assertEquals(LottieConstants.IterateForever, SplashIntroPresentation.iterations(Phase.LOOP))

        val introClip = LottieClipSpec.Frame(min = intro.minFrame, max = intro.maxFrame)
        val loopClip = LottieClipSpec.Frame(min = loop.minFrame, max = loop.maxFrame)
        assertEquals(0, introClip.min)
        assertEquals(90, introClip.max)
        assertEquals(90, loopClip.min)
        assertEquals(234, loopClip.max)
        assertEquals(
            "LottieCompositionSpec.RawRes splash_intro clipSpec intro minFrame=0 maxFrame=90",
            SplashIntroPresentation.clipDescription(Phase.INTRO),
        )
        assertEquals(
            "LottieCompositionSpec.RawRes splash_intro clipSpec loop minFrame=90 maxFrame=234",
            SplashIntroPresentation.clipDescription(Phase.LOOP),
        )
        assertTrue(
            SplashIntroPresentation.clipPlan().contains("intro minFrame=0 maxFrame=90"),
        )
        assertTrue(
            SplashIntroPresentation.clipPlan().contains("then loop minFrame=90 maxFrame=234"),
        )
    }

    @Test
    fun rawResSpecPointsAtSplashIntro() {
        val spec = LottieCompositionSpec.RawRes(R.raw.splash_intro)
        assertEquals(R.raw.splash_intro, spec.resId)
        assertNotEquals(0, spec.resId)
    }

    @Test
    fun splashIntroFileMatchesTheClipFrames() {
        val json = splashIntroFile().readText()
        assertEquals(60, jsonInt(json, "fr"))
        assertEquals(0, jsonInt(json, "ip"))
        assertEquals(234, jsonInt(json, "op"))
        assertEquals(1080, jsonInt(json, "w"))
        assertEquals(1080, jsonInt(json, "h"))
        val markers = Regex(""""tm":(\d+),"cm":"(intro|loop)","dr":(\d+)""")
        val found = markers.findAll(json).associate { match ->
            match.groupValues[2] to (match.groupValues[1].toInt() to match.groupValues[3].toInt())
        }
        val (introTm, introDr) = found["intro"] ?: error("intro marker missing")
        val (loopTm, loopDr) = found["loop"] ?: error("loop marker missing")
        assertEquals(SplashIntroPresentation.INTRO_MIN_FRAME, introTm)
        assertEquals(SplashIntroPresentation.INTRO_MAX_FRAME, introTm + introDr)
        assertEquals(SplashIntroPresentation.LOOP_MIN_FRAME, loopTm)
        assertEquals(SplashIntroPresentation.LOOP_MAX_FRAME, loopTm + loopDr)
        assertEquals(jsonInt(json, "op"), SplashIntroPresentation.LOOP_MAX_FRAME)
    }

    private fun jsonInt(json: String, key: String): Int {
        val match = Regex(""""$key":(-?\d+)""").find(json)
            ?: error("missing $key in splash_intro.json")
        return match.groupValues[1].toInt()
    }

    @Test
    fun failedCompositionKeepsTheStaticLogoUntilTheIntroWindow() {
        assertEquals(
            Phase.INTRO,
            SplashIntroPresentation.phase(
                introFinished = false,
                elapsedMs = SplashIntroPresentation.INTRO_MIN_VISIBLE_MS - 1,
                appReady = true,
                compositionFailed = true,
            ),
        )
        assertEquals(
            Phase.INTRO,
            SplashIntroPresentation.phase(
                introFinished = false,
                elapsedMs = 5_000,
                appReady = false,
                compositionFailed = true,
            ),
        )
        assertEquals(
            Phase.DONE,
            SplashIntroPresentation.phase(
                introFinished = false,
                elapsedMs = SplashIntroPresentation.INTRO_MIN_VISIBLE_MS,
                appReady = true,
                compositionFailed = true,
            ),
        )
    }

    private fun splashIntroFile(): File {
        val candidates = listOf(
            File("src/main/res/raw/splash_intro.json"),
            File("app/src/main/res/raw/splash_intro.json"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("splash_intro.json not found from ${File(".").absolutePath}")
    }
}
