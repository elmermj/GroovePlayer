package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun RunningText(
    text: String,
    modifier: Modifier = Modifier,
    speedPxPerSecond: Float = 120f,
    textStyle: TextStyle = LocalTextStyle.current,
    gap: Dp = 32.dp
) {
    var textWidth by remember { mutableStateOf(0) }
    var boxWidth by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val gapPx = with(density) { gap.toPx() }

    // Scroll when text is at least slightly wider than the box
    val shouldScroll = boxWidth > 0 && textWidth >= (boxWidth * 1.02f).toInt()

    val durationMillis = remember(textWidth, boxWidth) {
        if (!shouldScroll) 0
        else (((textWidth + gapPx) / speedPxPerSecond) * 1000f)
            .toInt()
            .coerceAtLeast(1)
    }

    val offsetX by if (shouldScroll) {
        val transition = rememberInfiniteTransition(label = "marquee")

        transition.animateFloat(
            initialValue = 0f,
            targetValue = -(textWidth + gapPx),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = durationMillis,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "offset"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { boxWidth = it.width }
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX
                }
        ) {
            Text(
                text = text,
                style = textStyle,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { textWidth = it.size.width }
            )

            if (shouldScroll) {
                Spacer(Modifier.width(gap))
                Text(
                    text = text,
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}