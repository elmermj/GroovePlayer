package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    height: Dp = 4.dp,
    dynamicSizeEnabled: Boolean = true,
    /** When set, overrides the default 2× active track height. */
    activeHeight: Dp? = null,
    showThumb: Boolean = false,
    thumbSizeRest: Dp = 8.dp,
    thumbSizeActive: Dp = 12.dp,
    /** Snap size changes (reduced-motion); colours still update. */
    reduceMotion: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val density = LocalDensity.current
    val isDragged by interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()

    val tapChannel = remember { Channel<Boolean>(Channel.UNLIMITED) }
    var isTapped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (tapped in tapChannel) {
            isTapped = tapped
            if (tapped) {
                kotlinx.coroutines.delay(150)
                isTapped = false
            }
        }
    }

    val isInteracting = isDragged || isTapped

    val defaultActiveHeight = if (dynamicSizeEnabled) height * 2.0f else height
    val targetTrackHeight =
        if (dynamicSizeEnabled && isInteracting) (activeHeight ?: defaultActiveHeight) else height

    val animSpec = if (reduceMotion) tween<Dp>(0) else tween<Dp>(150)
    val animatedHeight by animateDpAsState(
        targetValue = targetTrackHeight,
        animationSpec = animSpec,
        label = "sliderHeight"
    )
    val heightPx = with(density) { animatedHeight.toPx() }

    val targetThumb = when {
        !showThumb -> 0.dp
        isInteracting -> thumbSizeActive
        else -> thumbSizeRest
    }
    val animatedThumb by animateDpAsState(
        targetValue = targetThumb,
        animationSpec = animSpec,
        label = "thumbSize"
    )
    val thumbPx = with(density) { animatedThumb.toPx() }

    // Reserve space for the larger of active track or active thumb so layout does not jump.
    val reservedHeight = maxOf(
        activeHeight ?: defaultActiveHeight,
        if (showThumb) thumbSizeActive else 0.dp,
        height
    )

    var currentValue by remember { mutableFloatStateOf(value) }

    if (!isDragged && currentValue != value) {
        currentValue = value
    }

    val normalizedValue = ((currentValue - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(reservedHeight)
            .pointerInput(enabled, valueRange, tapChannel) {
                if (!enabled) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        tapChannel.trySend(true)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val newValue = ((down.position.x / width).coerceIn(0f, 1f) *
                                (valueRange.endInclusive - valueRange.start) + valueRange.start)
                                .coerceIn(valueRange.start, valueRange.endInclusive)

                            currentValue = newValue
                            onValueChange(newValue)
                            onValueChangeFinished?.invoke()
                        }
                    }
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput

                var dragStartInteraction: DragInteraction.Start? = null

                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartInteraction = DragInteraction.Start()
                        scope.launch {
                            dragStartInteraction?.let { interactionSource.emit(it) }
                        }

                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val initialValue = ((offset.x / width).coerceIn(0f, 1f) *
                            (valueRange.endInclusive - valueRange.start) + valueRange.start)
                            .coerceIn(valueRange.start, valueRange.endInclusive)

                        currentValue = initialValue
                        onValueChange(initialValue)
                    },
                    onDrag = { change, _ ->
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val newX = change.position.x.toFloat().coerceIn(0f, width)
                        val newValue = ((newX / width).coerceIn(0f, 1f) *
                            (valueRange.endInclusive - valueRange.start) + valueRange.start)
                            .coerceIn(valueRange.start, valueRange.endInclusive)

                        currentValue = newValue
                        onValueChange(newValue)
                    },
                    onDragEnd = {
                        dragStartInteraction?.let { start ->
                            val dragStop = DragInteraction.Stop(start)
                            scope.launch {
                                interactionSource.emit(dragStop)
                            }
                        }
                        dragStartInteraction = null
                        onValueChangeFinished?.invoke()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(reservedHeight)
                .align(Alignment.Center),
        ) {
            val width = size.width
            val trackTop = (size.height - heightPx) / 2f
            val filledWidth = width * normalizedValue
            val cornerRadius = heightPx / 2f

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, trackTop),
                size = Size(width, heightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )

            if (filledWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, trackTop),
                    size = Size(filledWidth, heightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            }

            if (showThumb && thumbPx > 0f) {
                val cx = (normalizedValue * width).coerceIn(thumbPx / 2f, width - thumbPx / 2f)
                val cy = size.height / 2f
                drawCircle(
                    color = activeColor,
                    radius = thumbPx / 2f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
