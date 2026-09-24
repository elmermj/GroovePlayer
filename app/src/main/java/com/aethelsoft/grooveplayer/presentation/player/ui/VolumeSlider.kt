package com.aethelsoft.grooveplayer.presentation.player.ui

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.rememberAdaptiveWindowInfo
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume1
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume2
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolume3
import com.aethelsoft.grooveplayer.utils.theme.icons.XVolumeOff
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.volumeMaxColor
import com.aethelsoft.grooveplayer.utils.theme.ui.volumeWarningColor

private val VolumeMinWidth = 140.dp
private val VolumeMaxPhone = 240.dp
private val VolumeMaxTablet = 280.dp
private val VolumeMaxLargeTablet = 320.dp
private val HitTargetHeight = 48.dp
private val IconRest = 24.dp
private val IconActive = 30.dp
private val TrackRest = 4.dp
private val TrackActive = 6.dp
private val ThumbRest = 8.dp
private val ThumbActive = 12.dp
private val IconGap = 8.dp

@Composable
private fun rememberVolumeReduceMotion(): Boolean {
    val accessibilityManager = LocalAccessibilityManager.current
    val context = LocalContext.current
    return remember(accessibilityManager) {
        val durationScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        } catch (_: Throwable) {
            1f
        }
        val transitionScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f,
            )
        } catch (_: Throwable) {
            1f
        }
        durationScale == 0f || transitionScale == 0f
    }
}

private fun volumeMaxFor(deviceType: DeviceType): Dp = when (deviceType) {
    DeviceType.PHONE -> VolumeMaxPhone
    DeviceType.TABLET -> VolumeMaxTablet
    DeviceType.LARGE_TABLET -> VolumeMaxLargeTablet
}

private fun volumeGlyph(isMuted: Boolean, volume: Float): ImageVector = when {
    isMuted || volume == 0f -> XVolumeOff
    volume < 0.2f -> XVolume
    volume < 0.4f -> XVolume1
    volume < 0.7f -> XVolume2
    else -> XVolume3
}

@Composable
private fun volumeTint(volume: Float): Color = when {
    volume > 0.85f && volume < 0.98f -> volumeWarningColor
    volume >= 0.98f -> volumeMaxColor
    else -> Color.White
}

/**
 * Single shared volume control for Phone / Tablet / LargeTablet FullPlayer and MiniPlayer.
 * Layouts only pass [modifier] (width constraints) and [backgroundColor].
 */
@Composable
fun VolumeSlider(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    opacity: Float = 1f
) {
    val volume by playerViewModel.volume.collectAsState()
    val isMuted by playerViewModel.isPlayerMuted.collectAsState()
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberVolumeReduceMotion()
    val windowInfo = rememberAdaptiveWindowInfo()
    val deviceMax = volumeMaxFor(windowInfo.deviceType)

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isDragged

    var previousVolume by remember { mutableStateOf(volume) }

    LaunchedEffect(isInteracting, volume) {
        if (previousVolume > 0f && volume == 0f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (previousVolume != volume && !isInteracting && previousVolume > 0f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (previousVolume < 0.80f && volume >= 0.80f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousVolume = volume
    }

    val motionSpec = if (reduceMotion) tween<Dp>(0) else tween(150)
    val glyphMs = if (reduceMotion) 0 else 150

    BoxWithConstraints(
        modifier = modifier
            .height(HitTargetHeight)
            .fillMaxWidth()
            .widthIn(min = VolumeMinWidth, max = deviceMax)
    ) {
        // Honour parent maxWidth when tighter than device max; never exceed available space.
        val totalWidth = when {
            maxWidth < VolumeMinWidth -> maxWidth
            else -> maxWidth.coerceIn(VolumeMinWidth, deviceMax)
        }

        val animatedIconSize by animateDpAsState(
            targetValue = if (isInteracting) IconActive else IconRest,
            animationSpec = motionSpec,
            label = "volumeIconSize"
        )

        // Track occupies remaining width to the left of the resting icon.
        val trackWidth = (totalWidth - IconGap - IconRest).coerceAtLeast(48.dp)

        val iconPositionX by animateDpAsState(
            targetValue = if (isInteracting) {
                (trackWidth / 2f) - (animatedIconSize / 2f)
            } else {
                totalWidth - animatedIconSize
            },
            animationSpec = motionSpec,
            label = "volumeIconX"
        )

        val shadowBg by androidx.compose.animation.animateColorAsState(
            targetValue = if (isInteracting) {
                backgroundColor.copy(alpha = 1f)
            } else {
                backgroundColor.copy(alpha = 0f)
            },
            animationSpec = if (reduceMotion) tween(0) else tween(150),
            label = "volumeShadowBg"
        )

        val displayVolume = if (isMuted) 0f else volume
        val activeColor =
            if (displayVolume > 0.85f) volumeWarningColor else GrooveTheme.colors.sliderFill

        Box(
            modifier = Modifier
                .width(totalWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                CustomSlider(
                    value = displayVolume,
                    onValueChange = { newValue ->
                        if (isMuted && newValue > 0f) {
                            playerViewModel.setMute(mute = false)
                        }
                        playerViewModel.setVolume(newValue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 0f..1f,
                    height = TrackRest,
                    activeHeight = TrackActive,
                    dynamicSizeEnabled = true,
                    showThumb = true,
                    thumbSizeRest = ThumbRest,
                    thumbSizeActive = ThumbActive,
                    reduceMotion = reduceMotion,
                    activeColor = activeColor,
                    inactiveColor = GrooveTheme.colors.sliderTrack.copy(alpha = 0.45f),
                    interactionSource = interactionSource
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = iconPositionX)
                    .size(animatedIconSize)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                if (isInteracting) {
                    Canvas(modifier = Modifier.size(animatedIconSize * 2.5f)) {
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
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

                val glyphKey = volumeGlyph(isMuted, volume)
                AnimatedContent(
                    targetState = glyphKey,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(glyphMs)) togetherWith
                            fadeOut(animationSpec = tween(glyphMs))
                    },
                    label = "volumeGlyph"
                ) { glyph ->
                    Icon(
                        imageVector = glyph,
                        contentDescription = if (isMuted || volume == 0f) "Unmute" else "Mute",
                        modifier = Modifier
                            .size(animatedIconSize)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isMuted || volume == 0f) {
                                        playerViewModel.setMute(mute = false)
                                        if (volume == 0f) {
                                            playerViewModel.setVolume(0.5f)
                                        }
                                    } else {
                                        playerViewModel.setMute(mute = true)
                                    }
                                }
                            ),
                        tint = volumeTint(if (isMuted) 0f else volume)
                    )
                }
            }
        }
    }
}
