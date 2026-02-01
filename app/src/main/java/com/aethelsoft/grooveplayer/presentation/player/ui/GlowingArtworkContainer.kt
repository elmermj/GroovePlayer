package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Audio-reactive glow container for tablet artwork with configurable visual effects.
 *
 * @param dominantColor The primary color extracted from artwork for glow tinting
 * @param visualization Real-time audio analysis data (bass, mid, treble, stereo, beat)
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

    // Balanced animations: responsive but smooth
    val bassGlow by animateFloatAsState(
        targetValue = visualization.bass,
        animationSpec = tween(75),
        label = "BassGlow"
    )

    val midGlow by animateFloatAsState(
        targetValue = visualization.mid,
        animationSpec = tween(70),
        label = "MidGlow"
    )

    val trebleGlow by animateFloatAsState(
        targetValue = visualization.treble,
        animationSpec = tween(60),
        label = "TrebleGlow"
    )

    val beatPulse by animateFloatAsState(
        targetValue = visualization.beat,
        animationSpec = tween(50),
        label = "BeatPulse"
    )

    val stereoBalance by animateFloatAsState(
        targetValue = visualization.stereoBalance,
        animationSpec = tween(120),
        label = "StereoBalance"
    )

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

                    // Calculate glow parameters using config
                    val baseIntensity = (bassGlow * 0.5f + midGlow * 0.3f + trebleGlow * 0.2f)
                    val glowAlpha =
                        (config.minAlpha + baseIntensity * config.intensityAlphaRange + beatPulse * config.beatAlphaBoost)
                            .coerceIn(0f, config.maxAlpha)

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