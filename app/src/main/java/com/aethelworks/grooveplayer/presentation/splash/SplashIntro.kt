package com.aethelworks.grooveplayer.presentation.splash

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.domain.splash.SplashIntroPresentation
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

private const val TAG = "SplashIntro"
private val SplashBlack = Color(0xFF000000)

/**
 * In-app splash drawn on top of [com.aethelworks.grooveplayer.MainActivity]'s
 * first frame, after the Android 12 system splash releases.
 *
 * Plays `res/raw/splash_intro` from [LottieCompositionSpec.RawRes]: frames 0–90
 * once, then frames 90–234 until [appReady]. A composition that fails to load
 * shows the launcher foreground on black and logs a warning.
 */
@Composable
fun SplashIntro(
    appReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_intro))
    val composition = result.value
    val failed = result.isFailure || (result.isComplete && composition == null)
    var introFinished by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val startedAt = remember { SystemClock.elapsedRealtime() }
    val description = stringResource(R.string.cd_splash_logo)

    LaunchedEffect(Unit) {
        while (true) {
            elapsedMs = SystemClock.elapsedRealtime() - startedAt
            delay(32)
        }
    }

    val phase = SplashIntroPresentation.phase(
        introFinished = introFinished,
        elapsedMs = elapsedMs,
        appReady = appReady,
        compositionFailed = failed,
    )

    LaunchedEffect(failed) {
        if (failed) {
            Log.w(
                TAG,
                "Splash composition failed to load splash_intro; showing static launcher foreground",
                result.error,
            )
        }
    }

    LaunchedEffect(composition) {
        val loaded = composition ?: return@LaunchedEffect
        Log.d(
            TAG,
            "LottieCompositionSpec.RawRes loaded splash_intro " +
                "durationFrames=${loaded.durationFrames} ${SplashIntroPresentation.clipPlan()}",
        )
    }

    LaunchedEffect(phase) {
        when (phase) {
            SplashIntroPresentation.Phase.INTRO,
            SplashIntroPresentation.Phase.LOOP,
            -> Log.d(TAG, SplashIntroPresentation.clipDescription(phase))
            SplashIntroPresentation.Phase.DONE ->
                Log.d(TAG, "splash handing over to the first screen")
        }
        if (phase == SplashIntroPresentation.Phase.DONE) onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBlack)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { tryAwaitRelease() })
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            failed -> SplashStaticLogo()
            composition != null -> SplashLottie(
                composition = composition,
                phase = phase,
                onIntroFinished = { introFinished = true },
            )
        }
    }
}

@Composable
private fun SplashLottie(
    composition: com.airbnb.lottie.LottieComposition,
    phase: SplashIntroPresentation.Phase,
    onIntroFinished: () -> Unit,
) {
    val clip = SplashIntroPresentation.clipFor(phase)
    val clipSpec = LottieClipSpec.Frame(min = clip.minFrame, max = clip.maxFrame)
    val iterations = if (phase == SplashIntroPresentation.Phase.LOOP) {
        LottieConstants.IterateForever
    } else {
        1
    }
    val animation = animateLottieCompositionAsState(
        composition = composition,
        isPlaying = phase != SplashIntroPresentation.Phase.DONE,
        restartOnPlay = false,
        clipSpec = clipSpec,
        iterations = iterations,
        ignoreSystemAnimatorScale = true,
    )
    var sawPlayback by remember { mutableStateOf(false) }
    var reportedIntro by remember { mutableStateOf(false) }
    // isAtEnd compares rounded progress to the clip end and can stay false after
    // the intro clip has stopped. Playback that has started and then stopped is
    // the end of the one-shot intro.
    LaunchedEffect(animation.isPlaying, animation.progress, phase) {
        if (animation.isPlaying) sawPlayback = true
        if (
            !reportedIntro &&
            phase == SplashIntroPresentation.Phase.INTRO &&
            sawPlayback &&
            !animation.isPlaying &&
            animation.progress > 0f
        ) {
            reportedIntro = true
            Log.d(TAG, "intro clip finished; ${SplashIntroPresentation.clipPlan()}")
            onIntroFinished()
        }
    }
    LottieAnimation(
        composition = composition,
        progress = { animation.progress },
        modifier = Modifier.fillMaxSize(),
        alignment = Alignment.Center,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun SplashStaticLogo() {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier.size(240.dp),
        contentScale = ContentScale.Fit,
    )
}
