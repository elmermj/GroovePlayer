package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SwipeableArtwork(
    size: Dp,
    artworkUrl: String?,
    swipeThresholdDp: Dp = 240.dp,
    dismissThresholdDp: Dp = 720.dp,
    onTap: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val swipeThresholdPx = remember {
        with(density) { swipeThresholdDp.toPx() }
    }
    val dismissThresholdPx = remember {
        with(density) { dismissThresholdDp.toPx() }
    }

    // ===== Drag state (SYNC, FAST) =====
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var thresholdHapticSent by remember { mutableStateOf(false) }

    // ===== Release animation =====
    val settleX = remember { Animatable(0f) }
    val settleY = remember { Animatable(0f) }

    val velocityTracker = remember { VelocityTracker() }

    val offsetX = dragX + settleX.value
    val offsetY = dragY + settleY.value

    fun rubberBand(value: Float): Float =
        value * 0.65f

    AnimatedContent(
        targetState = artworkUrl,
        transitionSpec = {
            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
        },
        label = "ArtworkCrossfade"
    ) { url ->
        AsyncImage(
            model = url,
            contentDescription = "Artwork",
            modifier = Modifier
                .size(size)
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(20.dp)

                    scaleX = 1f - abs(offsetX) / 1600f
                    scaleY = scaleX
                    alpha = 1f - abs(offsetY) / 1500f
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTap() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                            thresholdHapticSent = false
                            velocityTracker.resetTracking()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                            dragX = rubberBand(dragX + dragAmount.x)
                            dragY = rubberBand(dragY + dragAmount.y)

                            if (!thresholdHapticSent &&
                                (abs(dragX) > swipeThresholdPx ||
                                        abs(dragY) > dismissThresholdPx)
                            ) {
                                thresholdHapticSent = true
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val velocity = velocityTracker.calculateVelocity()

                                when {
                                    dragY > dismissThresholdPx || velocity.y > 2400f ->
                                        onDismiss()

                                    dragX < -swipeThresholdPx || velocity.x < -1500f ->
                                        onSwipeNext()

                                    dragX > swipeThresholdPx || velocity.x > 1500f ->
                                        onSwipePrevious()
                                }

                                settleX.animateTo(-dragX)
                                settleY.animateTo(-dragY)
                                dragX = 0f
                                dragY = 0f
                                settleX.snapTo(0f)
                                settleY.snapTo(0f)
                            }
                        }
                    )
                },
            contentScale = ContentScale.Crop
        )
    }
}