package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.palette.graphics.Palette
import coil3.Bitmap
import com.aethelsoft.grooveplayer.data.player.AudioVisualizationData
import com.aethelsoft.grooveplayer.presentation.player.layouts.GlowEffectConfig
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.WaveformUtils

/** Bass halo catches up over 140–180ms so emit jitter does not strobe. */
internal const val GLOW_BASS_MS = 160

/** Mids, including voice presence, ease over 120–160ms. */
internal const val GLOW_MID_MS = 140

/** Treble eases over 100–140ms. */
internal const val GLOW_TREBLE_MS = 120

/** Beat brightness rises quickly, then settles slowly. */
internal const val GLOW_BEAT_ATTACK_MS = 40
internal const val GLOW_BEAT_RELEASE_MS = 220

internal data class ArtworkGlowMotion(
    val bass: Float,
    val mid: Float,
    val treble: Float,
    val beat: Float,
    val stereo: Float,
    val glowAlpha: Float,
)

private class FloatRef(var value: Float)

/**
 * Shared glow motion for phone and tablet artwork.
 * Longer tweens absorb the ~66ms analysis emit interval. Beat attack and
 * release differ. [GlowEffectConfig.limitGlowAlphaStep] enforces the
 * photosensitive opacity cap (Comfort: 0.08 per frame).
 */
@Composable
internal fun rememberArtworkGlowMotion(
    visualization: AudioVisualizationData,
    config: GlowEffectConfig,
): ArtworkGlowMotion {
    val bassGlow by animateFloatAsState(
        targetValue = visualization.bass,
        animationSpec = tween(GLOW_BASS_MS),
        label = "BassGlow"
    )
    val midGlow by animateFloatAsState(
        targetValue = visualization.mid,
        animationSpec = tween(GLOW_MID_MS),
        label = "MidGlow"
    )
    val trebleGlow by animateFloatAsState(
        targetValue = visualization.treble,
        animationSpec = tween(GLOW_TREBLE_MS),
        label = "TrebleGlow"
    )
    val previousBeat = remember { FloatRef(visualization.beat) }
    val beatDuration = if (visualization.beat >= previousBeat.value) {
        GLOW_BEAT_ATTACK_MS
    } else {
        GLOW_BEAT_RELEASE_MS
    }
    previousBeat.value = visualization.beat
    val beatPulse by animateFloatAsState(
        targetValue = visualization.beat,
        animationSpec = tween(beatDuration),
        label = "BeatPulse"
    )
    val stereoBalance by animateFloatAsState(
        targetValue = visualization.stereoBalance,
        animationSpec = tween(120),
        label = "StereoBalance"
    )

    val baseIntensity = bassGlow * 0.5f + midGlow * 0.3f + trebleGlow * 0.2f
    val rawGlowAlpha = (
        config.minAlpha + baseIntensity * config.intensityAlphaRange + beatPulse * config.beatAlphaBoost
        ).coerceIn(0f, config.maxAlpha)
    val heldGlowAlpha = remember(config.minAlpha, config.maxGlowAlphaStep) {
        FloatRef(config.minAlpha)
    }
    val glowAlpha = config.limitGlowAlphaStep(heldGlowAlpha.value, rawGlowAlpha)
    heldGlowAlpha.value = glowAlpha

    return ArtworkGlowMotion(
        bass = bassGlow,
        mid = midGlow,
        treble = trebleGlow,
        beat = beatPulse,
        stereo = stereoBalance,
        glowAlpha = glowAlpha,
    )
}

/**
 * Audio-reactive glow container for tablet and large-tablet artwork.
 *
 * @param dominantColor The primary color extracted from artwork for glow tinting
 * @param visualization Audio analysis data (bass, mid, treble, stereo, beat)
 * @param config Glow effect configuration - use presets or customize
 * @param modifier Modifier for the container (must use graphicsLayer { clip = false })
 * @param content The artwork content to wrap with glow effect
 */
@Composable
fun GlowingArtworkContainer(
    dominantColor: Color,
    visualization: AudioVisualizationData,
    config: GlowEffectConfig = GlowEffectConfig.Tablet,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    deviceType: DeviceType,
){
    val density = LocalDensity.current
    val motion = rememberArtworkGlowMotion(visualization, config)
    val bassGlow = motion.bass
    val midGlow = motion.mid
    val trebleGlow = motion.treble
    val beatPulse = motion.beat
    val stereoBalance = motion.stereo
    val glowAlpha = motion.glowAlpha

    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .drawBehind {
                drawIntoCanvas { canvas ->
                    // Calculate colors for different frequency bands using config
                    val bassColor = dominantColor.copy(
                        red = dominantColor.red * config.bassColorRedMultiplier,
                        blue = dominantColor.blue * config.bassColorBlueMultiplier
                    )

                    val trebleColor = dominantColor.copy(
                        red = (dominantColor.red + config.trebleColorBoost).coerceAtMost(1f),
                        green = (dominantColor.green + config.trebleColorBoost).coerceAtMost(1f),
                        blue = (dominantColor.blue + config.trebleColorBoost).coerceAtMost(1f)
                    )

                    // Calculate blur radius using config
                    val blurRadiusPx = WaveformUtils(config, size).calculateBlurRadius(bassGlow, trebleGlow, beatPulse)

                    val cornerPx = with(density) { config.cornerRadius.toPx() }

                    // Layer 1: Bass layer with stereo-based horizontal extension (tapered edges)
                    WaveformUtils(config, size).drawBassLayerWithTaperedEdges(
                        isThresholdPassed = bassGlow > config.bassRenderThreshold,
                        canvas = canvas,
                        bassGlow = bassGlow,
                        stereoBalance = stereoBalance,
                        bassColor = bassColor,
                        glowAlpha = glowAlpha,
                        blurRadiusPx = blurRadiusPx,
                    )

                    // Layer 2: Mid layer (medium, dominant color)
                    WaveformUtils(config, size).drawMidLayer(
                        midGlow = midGlow,
                        canvas = canvas,
                        midColor = dominantColor,
                        glowAlpha = glowAlpha,
                        blurRadiusPx = blurRadiusPx,
                        cornerPx = cornerPx
                    )

                    // Layer 3: Treble layer (tight, bright)
                    WaveformUtils(config, size).drawTrebleLayer(
                        trebleGlow = trebleGlow,
                        canvas = canvas,
                        trebleColor = trebleColor,
                        glowAlpha = glowAlpha,
                        blurRadiusPx = blurRadiusPx,
                        cornerPx = cornerPx,
                        stereoBalance = stereoBalance
                    )

                    // Layer 4: White beat flash (rendered on top for maximum impact)
                    WaveformUtils(config, size).drawWhiteBeatFlash(
                        beatPulse = beatPulse,
                        canvas = canvas,
                        cornerPx = cornerPx,
                        blurRadiusPx = blurRadiusPx
                    )
                }
            }
    ) {
        content()
    }
}


fun adjustForTablets(

): Unit{

}

fun extractDominantColor(bitmap: Bitmap): Color {
    val palette = Palette.from(bitmap).generate()
    val swatch =
        palette.vibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch

    return swatch?.rgb?.let { Color(it) } ?: Color.White
}