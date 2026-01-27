package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
    style: TextStyle,
    color: Color,
    maxChars: Int,
    fadeWidth: Dp = 8.dp,
    speedDpPerSecond: Float = 24f,
) {
    val density = LocalDensity.current
    val fadePx = with(density){
        fadeWidth.toPx()
    }

    var textWidthPx by remember { mutableStateOf(0f) }
    var boxWidthPx by remember { mutableStateOf(0f) }

    val needsScroll = text.length > maxChars

    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(needsScroll, textWidthPx, boxWidthPx) {
        if (!needsScroll || textWidthPx <= boxWidthPx) return@LaunchedEffect

        val overflow = textWidthPx - boxWidthPx

        while (true) {
            offsetX.snapTo(0f)
            offsetX.animateTo(
                targetValue = -overflow,
                animationSpec = tween(
                    durationMillis = ((overflow / speedDpPerSecond) * 1000).toInt(),
                    easing = LinearEasing
                )
            )
            delay(800)
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = ((overflow / speedDpPerSecond) * 1000).toInt(),
                    easing = LinearEasing
                )
            )
            delay(1200)
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { boxWidthPx = it.width.toFloat() }
            .drawWithContent {
                drawContent()

                if (!needsScroll || textWidthPx <= boxWidthPx) return@drawWithContent

                val showLeftFade = offsetX.value < -1f
                val showRightFade = offsetX.value > -(textWidthPx - boxWidthPx - 1f)

                if (showLeftFade) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent
                        ),
                        size = Size(fadePx, size.height)
                    )
                }

                if (showRightFade) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black
                        ),
                        topLeft = Offset(size.width - fadePx, 0f),
                        size = Size(fadePx, size.height)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (needsScroll) text else text.take(maxChars),
            maxLines = 1,
            softWrap = false,
            style = style,
            color = color,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .onSizeChanged { textWidthPx = it.width.toFloat() }
        )
    }
}