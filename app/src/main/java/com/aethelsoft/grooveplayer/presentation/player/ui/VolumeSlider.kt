package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume1
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume2
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolumeOff
import com.aethelsoft.grooveplayer.utils.theme.ui.volumeMaxColor
import com.aethelsoft.grooveplayer.utils.theme.ui.volumeWarningColor

@Composable
fun VolumeSlider(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    opacity: Float = 1f
) {
    val volume by playerViewModel.volume.collectAsState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    // Shared interaction source for CustomSlider
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    
    // Track previous volume for haptic feedback when volume hits zero
    var previousVolume by remember { mutableStateOf(volume) }
    
    // Track if interacting (dragged or tapped)
    val isInteracting = isDragged
    
    // Haptic feedback when gesture is released or volume hits zero
    LaunchedEffect(isInteracting, volume) {
        // Haptic feedback when volume hits zero
        if (previousVolume > 0f && volume == 0f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        // Haptic feedback when gesture is released
        if (previousVolume != volume && !isInteracting && previousVolume > 0f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (previousVolume < 0.85f && volume >= 0.85f){
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousVolume = volume
    }

    BoxWithConstraints(
        modifier = modifier
            .height(48.dp)
    ) {
        val sliderWidth = if (maxWidth < 180.dp) maxWidth * 0.2f else 180.dp
        val iconSize = 24.dp
        val expandedIconSize = iconSize * 1.25f // 25% larger
        
        // Animate icon size based on interaction
        val animatedIconSize by animateDpAsState(
            targetValue = if (isInteracting) expandedIconSize else iconSize,
            animationSpec = tween(durationMillis = 150),
            label = "iconSize"
        )
        
        // Animate icon horizontal position: right side when not interacting, center of SLIDER when interacting
        val iconPositionX by animateDpAsState(
            targetValue = if (isInteracting) {
                // Center of the SLIDER (not the container)
                sliderWidth / 2f - animatedIconSize / 2f
            } else {
                // Right side of the slider
                sliderWidth + 12.dp
            },
            animationSpec = tween(durationMillis = 150),
            label = "iconPositionX"
        )

        // Slider row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Spacer(Modifier.weight(1f))
                CustomSlider(
                    value = volume,
                    onValueChange = { playerViewModel.setVolume(it) },
                    modifier = Modifier.width(sliderWidth),
                    valueRange = 0f..1f,
                    height = 4.dp,
                    activeColor =  if(volume > 0.85f) volumeWarningColor else Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.3f),
                    interactionSource = interactionSource
                )
                Spacer(Modifier.weight(1f))
            }
        }

        val shadowPositionX by animateDpAsState(
            targetValue = if (isInteracting) {
                sliderWidth / 2f - animatedIconSize * 8f
            } else {
                sliderWidth + 12.dp
            },
            animationSpec = tween(durationMillis = 150),
            label = "shadowPositionX"
        )

        val shadowBg by animateColorAsState(
            targetValue = if (isInteracting) {
                backgroundColor.copy(alpha = 1f)
            } else {
                backgroundColor.copy(alpha = 0f)
            },
            animationSpec = tween(durationMillis = 150),
            label = "shadowBg"
        )

        /* Volume icon that moves between right side and center */
        Column{
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .offset(x = iconPositionX, y = 0.dp)
                    .size(animatedIconSize)
            ) {
                if(isInteracting) {
                    Canvas(
                        modifier = Modifier
                            .size(animatedIconSize)
                    ) {
                        val radius = size.minDimension / 1f

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    shadowBg.copy(alpha = 1f * opacity),
                                    shadowBg.copy(alpha = 1f * opacity),
                                    shadowBg.copy(alpha = 0.75f * opacity),
                                    shadowBg.copy(alpha = 0.5f * opacity),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                    }
                }
                // Volume icon with morphing based on volume level
                AnimatedContent(
                    targetState = volume,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(0)) togetherWith fadeOut(
                            animationSpec = tween(
                                0
                            )
                        )
                    },
                    label = "volumeIcon"
                ) { currentVolume ->
                    IconButton(
                        onClick = {
                            if (playerViewModel.isPlayerMuted.value || currentVolume == 0f){
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                playerViewModel.setMute(mute = false)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                playerViewModel.setMute(mute = true)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when {
                                currentVolume == 0f -> XVolumeOff
                                currentVolume < 0.3f -> XVolume
                                currentVolume < 0.7f -> XVolume1
                                else -> XVolume2
                            },
                            contentDescription = "Volume",
                            modifier = Modifier.size(animatedIconSize),
                            tint = when {
                                currentVolume > 0.85f && currentVolume < 0.97f -> volumeWarningColor
                                currentVolume >= 0.98f -> volumeMaxColor
                                else -> Color.White
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}