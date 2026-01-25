package com.aethelsoft.grooveplayer.utils.theme.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.S_PADDING
import kotlin.math.sin

@Composable
fun AudioWaveAnimation(
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White.copy(alpha = 0.8f),
    waveHeight: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
    speed: Float = 1f,
    edgeFadeWidth: Dp = 32.dp // 👈 blur width
) {
    val density = LocalDensity.current
    val waveHeightPx = with(density) { waveHeight.toPx() }
    val strokePx = with(density) { strokeWidth.toPx() }
    val edgeFadePx = with(density) { edgeFadeWidth.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1200 / speed).toInt(),
                easing = LinearEasing
            )
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val waveCount = 3
        val wavelength = width / 1.2f

        repeat(waveCount) { i ->
            val localPhase = phase + i * 1.2f
            val amplitude = waveHeightPx * (0.3f + i * 0.15f)
            val alpha = 0.6f - i * 0.15f

            val path = Path().apply {
                moveTo(0f, centerY)
                var x = 0f
                while (x <= width) {
                    val y = centerY +
                            sin((x / wavelength) * 2f * Math.PI + localPhase).toFloat() * amplitude
                    lineTo(x, y)
                    x += 6f
                }
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0f * alpha), // left edge
                        waveColor.copy(alpha = 0.75f * alpha), // center-left
                        waveColor.copy(alpha = alpha),          // center
                        waveColor.copy(alpha = 0.75f * alpha),  // center-right
                        waveColor.copy(alpha = 0f * alpha), // right edge
                    )
                ),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}