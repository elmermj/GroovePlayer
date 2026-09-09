package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberDeviceType

/** Corner radius used by [SwipeableArtwork] / full-player art. */
val PlayerArtworkCornerRadius: Dp = 20.dp

/**
 * Artwork size matching [SwipeableArtwork] in each full-player layout:
 * - Phone: min(screenH * 0.6, screenW * 0.8) - [S_PADDING]
 * - Tablet / large tablet: min(screenH * 0.4, screenW * 0.5) - [S_PADDING]
 */
@Composable
fun rememberPlayerArtworkSize(): Dp {
    val deviceType = rememberDeviceType()
    val container = LocalWindowInfo.current.containerSize
    // S_PADDING is a @Composable getter; read it outside the remember lambda.
    val sPadding = S_PADDING
    return remember(deviceType, container.width, container.height, sPadding) {
        val screenHeight = container.height.dp
        val screenWidth = container.width.dp
        val maxArtworkHeight = when (deviceType) {
            DeviceType.PHONE -> minOf(screenHeight * 0.6f, screenWidth * 0.8f)
            DeviceType.TABLET,
            DeviceType.LARGE_TABLET -> minOf(screenHeight * 0.4f, screenWidth * 0.5f)
        }
        maxArtworkHeight - sPadding
    }
}
