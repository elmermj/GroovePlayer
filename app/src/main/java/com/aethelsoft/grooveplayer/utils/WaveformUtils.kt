package com.aethelsoft.grooveplayer.utils

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.aethelsoft.grooveplayer.presentation.player.layouts.GlowEffectConfig

class WaveformUtils (
    val config: GlowEffectConfig,
    val size: Size,
){

    fun calculateHorizontalExtension(
        stereoBalance: Float,
        maxHorizontalExtension: Float
    ) : List<Float> {
        val leftExtension = if (stereoBalance <= 0) {
            maxHorizontalExtension * (1f + kotlin.math.abs(stereoBalance))
        } else {
            maxHorizontalExtension * (1f - stereoBalance)
        }

        val rightExtension = if (stereoBalance >= 0) {
            maxHorizontalExtension * (1f + stereoBalance)
        } else {
            maxHorizontalExtension * (1f + stereoBalance)
        }
        return listOf(leftExtension, rightExtension)
    }

    fun drawBassLayerWithTaperedEdges(
        isThresholdPassed: Boolean,
        canvas: Canvas,
        bassGlow: Float,
        stereoBalance: Float,
        bassColor: Color,
        glowAlpha: Float,
        blurRadiusPx: Float
    ): Canvas {
        if (isThresholdPassed) {

            val horizontalExtension =  calculateHorizontalExtension(
                stereoBalance = stereoBalance,
                maxHorizontalExtension = size.width * config.bassHorizontalExtension
            )

            // Draw bass as ellipse for pointed/tapered edges
            val bassPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = bassColor
                    .copy(alpha = glowAlpha * config.bassLayerAlpha * bassGlow)
                    .toArgb()
                maskFilter = BlurMaskFilter(
                    blurRadiusPx * config.bassLayerBlurScale,
                    BlurMaskFilter.Blur.NORMAL
                )
            }

            // Draw as oval/ellipse for tapered horizontal edges
            canvas.nativeCanvas.drawOval(
                -horizontalExtension[0],
                0f,
                size.width + horizontalExtension[1],
                size.height,
                bassPaint
            )
        }
        return canvas
    }

    fun drawMidLayer(
        midGlow: Float,
        canvas: Canvas,
        midColor: Color,
        glowAlpha: Float,
        blurRadiusPx: Float,
        cornerPx: Float
    ): Canvas{
        if (midGlow > config.midRenderThreshold) {
            val midPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = midColor
                    .copy(alpha = glowAlpha * config.midLayerAlpha * midGlow)
                    .toArgb()
                maskFilter = BlurMaskFilter(
                    blurRadiusPx * config.midLayerBlurScale,
                    BlurMaskFilter.Blur.NORMAL
                )
            }

            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerPx,
                cornerPx,
                midPaint
            )
        }
        return canvas
    }

    fun drawTrebleLayer(
        trebleGlow: Float,
        canvas: Canvas,
        trebleColor: Color,
        glowAlpha: Float,
        blurRadiusPx: Float,
        cornerPx: Float,
        stereoBalance: Float
    ): Canvas{
        if (trebleGlow > config.trebleRenderThreshold) {
            val treblePaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = trebleColor
                    .copy(alpha = glowAlpha * config.trebleLayerAlpha * trebleGlow)
                    .toArgb()
                maskFilter = BlurMaskFilter(
                    blurRadiusPx * config.trebleLayerBlurScale,
                    BlurMaskFilter.Blur.NORMAL
                )
            }

            val stereoOffsetX =
                size.width * config.stereoBalanceTrebleScale * -stereoBalance

            canvas.nativeCanvas.drawRoundRect(
                -stereoOffsetX,
                0f,
                size.width - stereoOffsetX,
                size.height,
                cornerPx,
                cornerPx,
                treblePaint
            )
        }
        return canvas
    }

    fun drawWhiteBeatFlash(
        beatPulse: Float,
        canvas: Canvas,
        blurRadiusPx: Float,
        cornerPx: Float
    ):Canvas {
        if (config.enableBeatFlash && beatPulse > config.beatFlashThreshold) {
            val beatFlashPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                // Pure white with beat-modulated alpha
                color = Color.White
                    .copy(alpha = config.beatFlashAlpha * beatPulse)
                    .toArgb()
                maskFilter = BlurMaskFilter(
                    blurRadiusPx * config.beatFlashBlurScale,
                    BlurMaskFilter.Blur.NORMAL
                )
            }

            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerPx,
                cornerPx,
                beatFlashPaint
            )
        }
        return canvas
    }

    fun calculateBlurRadius(
        bassGlow: Float,
        trebleGlow: Float,
        beatPulse: Float
    ): Float {
        val baseBlurRadius = size.minDimension * config.baseBlurMultiplier
        val bassExpansion =
            size.minDimension * config.bassExpansionMultiplier * bassGlow
        val trebleTightness =
            -size.minDimension * config.trebleTightnessMultiplier * trebleGlow
        val beatExpansion =
            size.minDimension * config.beatExpansionMultiplier * beatPulse
        return (baseBlurRadius + bassExpansion + trebleTightness + beatExpansion)
                .coerceAtLeast(size.minDimension * config.minBlurMultiplier)
    }
}