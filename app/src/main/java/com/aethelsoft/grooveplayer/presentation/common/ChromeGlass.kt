package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

/** Extra fade drawn past the chrome edge so list content softens underneath. */
val ChromeGlassFadeExtension: Dp = 52.dp

/**
 * Top chrome glass: opaque through the bar, then a long fade into content below.
 * Uses [GrooveColors.edgeGradient], not canvas, so the veil stays distinct from the page.
 * Fade is drawn past layout bounds ([clip] = false) so it does not steal touches.
 */
@Composable
fun Modifier.grooveTopChromeGlass(
    edge: Color = GrooveTheme.colors.edgeGradient,
    fadeExtension: Dp = ChromeGlassFadeExtension,
): Modifier {
    val fadePx = with(LocalDensity.current) { fadeExtension.toPx() }
    return this
        .graphicsLayer { clip = false }
        .drawWithCache {
            val totalH = size.height + fadePx
            val barEnd = (size.height / totalH).coerceIn(0f, 1f)
            val brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to edge.copy(alpha = 0.94f),
                    0.35f * barEnd to edge.copy(alpha = 0.88f),
                    0.70f * barEnd to edge.copy(alpha = 0.72f),
                    barEnd to edge.copy(alpha = 0.42f),
                    barEnd + (1f - barEnd) * 0.40f to edge.copy(alpha = 0.18f),
                    barEnd + (1f - barEnd) * 0.72f to edge.copy(alpha = 0.06f),
                    1.00f to Color.Transparent,
                ),
                startY = 0f,
                endY = totalH,
            )
            onDrawBehind {
                drawRect(brush = brush, size = Size(size.width, totalH))
            }
        }
}

/**
 * Frosted peek strip for the song-details sheet: translucent surface over the player.
 */
@Composable
fun Modifier.groovePeekFrostedGlass(
    edge: Color = GrooveTheme.colors.edgeGradient,
    surface: Color = GrooveTheme.colors.surface,
): Modifier {
    return this.drawWithCache {
        val brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to surface.copy(alpha = 0.52f),
                0.35f to edge.copy(alpha = 0.48f),
                0.75f to edge.copy(alpha = 0.58f),
                1.00f to surface.copy(alpha = 0.62f),
            ),
        )
        val highlight = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f),
                Color.Transparent,
            ),
            endY = size.height * 0.45f,
        )
        onDrawBehind {
            drawRect(brush = brush)
            drawRect(brush = highlight)
            // Top hairline for glass edge
            drawRect(
                color = Color.White.copy(alpha = 0.18f),
                size = Size(size.width, 1.dp.toPx()),
            )
        }
    }
}

/**
 * Bottom chrome glass: long fade above the mini-player into a near-opaque base.
 * Uses [GrooveColors.edgeGradient], not canvas, so the veil stays distinct from the page.
 * Fade is drawn past layout bounds ([clip] = false) so it does not steal touches.
 */
@Composable
fun Modifier.grooveBottomChromeGlass(
    edge: Color = GrooveTheme.colors.edgeGradient,
    fadeExtension: Dp = ChromeGlassFadeExtension,
): Modifier {
    val fadePx = with(LocalDensity.current) { fadeExtension.toPx() }
    return this
        .graphicsLayer { clip = false }
        .drawWithCache {
            val totalH = size.height + fadePx
            val contentStart = (fadePx / totalH).coerceIn(0f, 1f)
            val brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    contentStart * 0.25f to edge.copy(alpha = 0.06f),
                    contentStart * 0.50f to edge.copy(alpha = 0.18f),
                    contentStart * 0.75f to edge.copy(alpha = 0.38f),
                    contentStart to edge.copy(alpha = 0.58f),
                    contentStart + (1f - contentStart) * 0.45f to edge.copy(alpha = 0.82f),
                    1.00f to edge.copy(alpha = 0.94f),
                ),
                startY = -fadePx,
                endY = size.height,
            )
            onDrawBehind {
                drawRect(
                    brush = brush,
                    topLeft = Offset(0f, -fadePx),
                    size = Size(size.width, totalH),
                )
            }
        }
}
