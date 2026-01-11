package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Alignment
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
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val density = LocalDensity.current
    val isDragged by interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()
    
    // Channel to communicate tap events from pointerInput to composable
    val tapChannel = remember { Channel<Boolean>(Channel.UNLIMITED) }
    
    // Track if slider is being tapped (separate from drag)
    var isTapped by remember { mutableStateOf(false) }
    
    // Listen to tap events from pointerInput
    LaunchedEffect(Unit) {
        for (tapped in tapChannel) {
            isTapped = tapped
            if (tapped) {
                kotlinx.coroutines.delay(150)
                isTapped = false
            }
        }
    }
    
    // Combined interaction state: either dragged or tapped
    val isInteracting = isDragged || isTapped
    
    // Maximum height: 2x if dynamic size enabled, otherwise same as height
    val maxHeight = if (dynamicSizeEnabled) height * 2.0f else height
    
    // Animate height: 100% larger when interacting (2x) if dynamic size is enabled
    val animatedHeight by animateDpAsState(
        targetValue = if (dynamicSizeEnabled && isInteracting) maxHeight else height,
        animationSpec = tween(durationMillis = 150),
        label = "sliderHeight"
    )
    val heightPx = with(density) { animatedHeight.toPx() }
    
    var currentValue by remember { mutableFloatStateOf(value) }
    
    // Update currentValue when value changes externally (but not during drag)
    if (!isDragged && currentValue != value) {
        currentValue = value
    }
    
    val normalizedValue = ((currentValue - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    // Reserve space for maximum height to prevent layout shifts
    Box(
        modifier = modifier
            .height(maxHeight)
            .pointerInput(enabled, valueRange, tapChannel) {
                if (!enabled) return@pointerInput
                
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        tapChannel.trySend(true)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            // Tap completed
                            val width = size.width.toFloat()
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
                        
                        val width = size.width.toFloat()
                        val initialValue = ((offset.x / width).coerceIn(0f, 1f) * 
                            (valueRange.endInclusive - valueRange.start) + valueRange.start)
                            .coerceIn(valueRange.start, valueRange.endInclusive)
                        
                        currentValue = initialValue
                        onValueChange(initialValue)
                    },
                    onDrag = { change, _ ->
                        val width = size.width.toFloat()
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
                .height(animatedHeight)
                .align(Alignment.Center),
        ) {
            val width = size.width
            val filledWidth = width * normalizedValue
            val cornerRadius = heightPx / 2
            
            // Draw inactive track
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset.Zero,
                size = Size(width, heightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
            
            // Draw active track
            if (filledWidth > 0) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset.Zero,
                    size = Size(filledWidth, heightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    }
}
