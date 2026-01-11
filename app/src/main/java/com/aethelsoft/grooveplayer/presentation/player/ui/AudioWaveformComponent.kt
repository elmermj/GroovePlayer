package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioWaveformComponent(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    progress: Float
) {
    // Animated time value for frequency visualization
    var animationTime by remember { mutableStateOf(0f) }

    // Update animation time when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(50) // Update every 50ms for smooth animation
            animationTime += 0.1f
            if (animationTime > 100f) animationTime = 0f
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = 64 // More bars for better frequency representation
        val barWidth = width / barCount
        val spacing = barWidth * 0.25f
        val actualBarWidth = barWidth - spacing

        // Base frequency patterns for different frequency ranges
        // Lower frequencies (bass) on left, higher frequencies (treble) on right
        for (i in 0 until barCount) {
            val x = i * barWidth + spacing / 2
            val normalizedPos = i.toFloat() / barCount

            // Calculate frequency-based amplitude
            // Lower frequencies (left side) have stronger bass patterns
            // Higher frequencies (right side) have more varied patterns
            val frequencyFactor = if (normalizedPos < 0.3f) {
                // Bass frequencies - stronger, slower changes
                0.4f + 0.4f * sin(animationTime * 0.5f + i * 0.2f)
            } else if (normalizedPos < 0.7f) {
                // Mid frequencies - varied patterns
                0.2f + 0.5f * (sin(animationTime * 1.2f + i * 0.3f) * 0.6f + cos(animationTime * 0.8f + i * 0.4f) * 0.4f)
            } else {
                // High frequencies - faster, more random-like patterns
                0.1f + 0.4f * sin(animationTime * 2f + i * 0.5f) * cos(animationTime * 1.5f + i * 0.3f)
            }

            val barHeight = if (isPlaying) {
                // Animated frequency bars
                height * (0.15f + frequencyFactor.coerceIn(0f, 0.85f))
            } else {
                // Static when paused
                height * 0.15f
            }

            // Color based on frequency range
            val barColor = when {
                normalizedPos < 0.3f -> Color.White // Bass - white
                normalizedPos < 0.7f -> Color.White.copy(alpha = 0.9f) // Mid - slightly transparent
                else -> Color.White.copy(alpha = 0.8f) // High - more transparent
            }

            // Draw frequency bar (centered vertically)
            drawRect(
                color = barColor,
                topLeft = Offset(x, (height - barHeight) / 2),
                size = Size(actualBarWidth, barHeight)
            )
        }
    }
}