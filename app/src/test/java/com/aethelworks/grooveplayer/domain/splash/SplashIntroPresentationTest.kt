package com.aethelworks.grooveplayer.domain.splash

import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.domain.splash.SplashIntroPresentation.Phase
import com.airbnb.lottie.LottieConstants
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import org.json.JSONObject
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
        val json = JSONObject(splashIntroFile().readText())
        assertEquals(60, json.getInt("fr"))
        assertEquals(0, json.getInt("ip"))
        assertEquals(234, json.getInt("op"))
        assertEquals(1080, json.getInt("w"))
        assertEquals(1080, json.getInt("h"))
        val markers = json.getJSONArray("markers")
        var introTm = -1
        var introDr = -1
        var loopTm = -1
        var loopDr = -1
        for (i in 0 until markers.length()) {
            val marker = markers.getJSONObject(i)
            when (marker.getString("cm")) {
                "intro" -> {
                    introTm = marker.getInt("tm")
                    introDr = marker.getInt("dr")
                }
                "loop" -> {
                    loopTm = marker.getInt("tm")
                    loopDr = marker.getInt("dr")
                }
            }
        }
        assertEquals(SplashIntroPresentation.INTRO_MIN_FRAME, introTm)
        assertEquals(SplashIntroPresentation.INTRO_MAX_FRAME, introTm + introDr)
        assertEquals(SplashIntroPresentation.LOOP_MIN_FRAME, loopTm)
        assertEquals(SplashIntroPresentation.LOOP_MAX_FRAME, loopTm + loopDr)
        assertEquals(json.getInt("op"), SplashIntroPresentation.LOOP_MAX_FRAME)
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
