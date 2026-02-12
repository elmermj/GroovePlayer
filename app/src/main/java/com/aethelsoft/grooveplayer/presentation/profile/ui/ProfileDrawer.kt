package com.aethelsoft.grooveplayer.presentation.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING


@Composable
fun ProfileDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigateToShare: () -> Unit = {},
    deviceType: DeviceType,
) {
    val drawerWidth = when (deviceType) {
        DeviceType.TABLET -> 360.dp
        DeviceType.LARGE_TABLET -> 420.dp
        else -> 0.dp
    }
    // Wrap in Box so scrim and drawer overlay each other (not stack vertically).
    // Transparent area outside the panel is clickable to close; panel consumes taps so they don't close.
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onClose() }
            )
        }
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClose() }
            ) {
                // Left-side drawer content - consumes taps so panel taps don't close
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
                        .align(Alignment.CenterStart)
                        .background(Color.Black)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* consume - don't close when tapping panel */ }
                ) {
                    ProfileDrawerContent(
                        deviceType = deviceType,
                        onNavigateToShare = onNavigateToShare,
                        onClose = onClose
                    )
                }
            }
        }
    }
}