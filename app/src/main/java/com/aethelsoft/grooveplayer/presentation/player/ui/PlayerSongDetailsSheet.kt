package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.navigationBarsInset
import com.aethelsoft.grooveplayer.presentation.player.PlayerSongDetailsSheetState
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val PeekContentHeight = 96.dp
private val SheetTopRadius = 20.dp
private val DetailsBottomContentGap = 48.dp
private val ExpandedContentTopGap = 8.dp

/** Bouncy settle used for peek appear, expand, and close. */
private val SheetBounceSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium,
)

@Composable
fun PlayerSongDetailsSheet(
    state: PlayerSongDetailsSheetState,
    song: Song?,
    onStateChange: (PlayerSongDetailsSheetState) -> Unit,
    expandedTopInsetPx: Float,
    modifier: Modifier = Modifier,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val density = LocalDensity.current
    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val navBarsInset = navigationBarsInset()
    val peekHeightPx = with(density) { (PeekContentHeight + navBarsInset).toPx() }
    val detailsBottomPadding = DetailsBottomContentGap + navBarsInset
    // Content clears the overlay miniplayer; the sheet itself is edge-to-edge.
    val detailsTopPadding = with(density) {
        expandedTopInsetPx.toDp() + ExpandedContentTopGap
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val onStateChangeUpdated by rememberUpdatedState(onStateChange)
    val stateUpdated by rememberUpdatedState(state)

    fun offsetFor(target: PlayerSongDetailsSheetState): Float = when (target) {
        PlayerSongDetailsSheetState.Hidden -> screenHeightPx
        PlayerSongDetailsSheetState.Peek -> screenHeightPx - peekHeightPx
        PlayerSongDetailsSheetState.Expanded -> 0f
    }

    var isRendered by remember { mutableStateOf(state != PlayerSongDetailsSheetState.Hidden) }
    val offsetY = remember { Animatable(screenHeightPx) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var showDetailsContent by remember {
        mutableStateOf(state == PlayerSongDetailsSheetState.Expanded)
    }
    // First swipe-down at the scroll top only "arms" the close; the second one closes.
    var collapseArmed by remember { mutableStateOf(false) }

    LaunchedEffect(state, screenHeightPx, expandedTopInsetPx, peekHeightPx) {
        // Clear any in-flight drag so reopen after dismiss is a single swipe.
        dragAccum = 0f
        collapseArmed = false

        when (state) {
            PlayerSongDetailsSheetState.Hidden -> {
                if (!isRendered) return@LaunchedEffect
                showDetailsContent = false
                offsetY.animateTo(offsetFor(PlayerSongDetailsSheetState.Hidden), SheetBounceSpring)
                isRendered = false
            }
            PlayerSongDetailsSheetState.Peek -> {
                showDetailsContent = false
                if (!isRendered) {
                    isRendered = true
                    offsetY.snapTo(offsetFor(PlayerSongDetailsSheetState.Hidden))
                }
                offsetY.animateTo(offsetFor(PlayerSongDetailsSheetState.Peek), SheetBounceSpring)
            }
            PlayerSongDetailsSheetState.Expanded -> {
                showDetailsContent = true
                if (!isRendered) {
                    isRendered = true
                    offsetY.snapTo(offsetFor(PlayerSongDetailsSheetState.Hidden))
                }
                offsetY.animateTo(offsetFor(PlayerSongDetailsSheetState.Expanded), SheetBounceSpring)
                listState.scrollToItem(0)
            }
        }
    }

    if (!isRendered) return

    fun settleFromDrag() {
        val current = offsetY.value
        val peek = offsetFor(PlayerSongDetailsSheetState.Peek)
        val expanded = offsetFor(PlayerSongDetailsSheetState.Expanded)
        val hidden = offsetFor(PlayerSongDetailsSheetState.Hidden)
        val midPeekExpanded = (peek + expanded) / 2f
        val midPeekHidden = (peek + hidden) / 2f
        val next = when (stateUpdated) {
            PlayerSongDetailsSheetState.Peek -> when {
                current < midPeekExpanded || dragAccum < -64f -> PlayerSongDetailsSheetState.Expanded
                current > midPeekHidden || dragAccum > 64f -> PlayerSongDetailsSheetState.Hidden
                else -> PlayerSongDetailsSheetState.Peek
            }
            PlayerSongDetailsSheetState.Expanded -> when {
                current > midPeekExpanded || dragAccum > 100f -> {
                    if (collapseArmed) {
                        PlayerSongDetailsSheetState.Hidden
                    } else {
                        // First swipe-down: arm and bounce back instead of closing.
                        collapseArmed = true
                        PlayerSongDetailsSheetState.Expanded
                    }
                }
                else -> PlayerSongDetailsSheetState.Expanded
            }
            PlayerSongDetailsSheetState.Hidden -> PlayerSongDetailsSheetState.Hidden
        }
        dragAccum = 0f
        if (next == stateUpdated) {
            // State unchanged → LaunchedEffect won't re-run; settle back to the anchor manually.
            scope.launch {
                offsetY.animateTo(offsetFor(next), SheetBounceSpring)
            }
        }
        onStateChangeUpdated(next)
    }

    fun applyDrag(delta: Float) {
        dragAccum += delta
        val min = offsetFor(PlayerSongDetailsSheetState.Expanded)
        val max = offsetFor(PlayerSongDetailsSheetState.Hidden)
        scope.launch {
            offsetY.snapTo((offsetY.value + delta).coerceIn(min, max))
        }
    }

    val nestedScrollConnection = remember(listState, screenHeightPx, peekHeightPx, expandedTopInsetPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (stateUpdated != PlayerSongDetailsSheetState.Expanded) return Offset.Zero
                // Scrolling back into the content disarms the pending close.
                if (collapseArmed && listState.canScrollBackward) collapseArmed = false
                if (available.y <= 0f || listState.canScrollBackward) return Offset.Zero
                applyDrag(available.y)
                return Offset(0f, available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (stateUpdated != PlayerSongDetailsSheetState.Expanded) return Offset.Zero
                if (available.y <= 0f || listState.canScrollBackward) return Offset.Zero
                applyDrag(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (stateUpdated != PlayerSongDetailsSheetState.Expanded) return Velocity.Zero
                if (listState.canScrollBackward || available.y <= 0f) {
                    if (dragAccum != 0f) settleFromDrag()
                    return Velocity.Zero
                }
                settleFromDrag()
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (dragAccum != 0f) settleFromDrag()
                return Velocity.Zero
            }
        }
    }

    val peek = offsetFor(PlayerSongDetailsSheetState.Peek)
    val expanded = offsetFor(PlayerSongDetailsSheetState.Expanded)
    val hiddenOffset = offsetFor(PlayerSongDetailsSheetState.Hidden)
    val progress = ((peek - offsetY.value) / (peek - expanded).coerceAtLeast(1f)).coerceIn(0f, 1f)
    // 0 when fully hidden → 1 once the peek is fully revealed; drives the scrim
    // so it fades in/out together with the sheet instead of popping.
    val peekReveal =
        ((hiddenOffset - offsetY.value) / (hiddenOffset - peek).coerceAtLeast(1f)).coerceIn(0f, 1f)
    // 30% black scrim at peek, deepening to 45% when fully expanded.
    val scrimAlpha = 0.30f * peekReveal + 0.15f * progress
    val isPeekVisual = !showDetailsContent && progress < 0.35f
    val sheetShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim: tap closes; while peeking, swipe up expands ("swipe up again").
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(stateUpdated) {
                    var totalX = 0f
                    var totalY = 0f
                    var dragged = false
                    detectDragGestures(
                        onDragStart = {
                            totalX = 0f
                            totalY = 0f
                            dragged = false
                        },
                        onDragEnd = {
                            when {
                                stateUpdated == PlayerSongDetailsSheetState.Peek &&
                                        abs(totalY) > abs(totalX) &&
                                        totalY < -64f -> {
                                    onStateChangeUpdated(PlayerSongDetailsSheetState.Expanded)
                                }

                                !dragged || (abs(totalX) < 12f && abs(totalY) < 12f) -> {
                                    onStateChangeUpdated(PlayerSongDetailsSheetState.Hidden)
                                }

                                stateUpdated == PlayerSongDetailsSheetState.Peek &&
                                        abs(totalY) > abs(totalX) &&
                                        totalY > 64f -> {
                                    onStateChangeUpdated(PlayerSongDetailsSheetState.Hidden)
                                }

                                else -> {
                                    onStateChangeUpdated(PlayerSongDetailsSheetState.Hidden)
                                }
                            }
                        },
                        onDragCancel = { },
                    ) { _, dragAmount ->
                        dragged = true
                        totalX += dragAmount.x
                        totalY += dragAmount.y
                    }
                },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .clip(sheetShape)
                .then(
                    if (isPeekVisual) {
                        // Gradient over the visible peek area: 30% black at the top
                        // fading to solid black at the bottom edge. Past the peek
                        // height the brush clamps to solid black, so any extra area
                        // revealed while dragging up stays opaque.
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.30f), Color.Black),
                                startY = 0f,
                                endY = peekHeightPx,
                            ),
                        )
                    } else {
                        // Solid full-bleed canvas so expanded covers the entire screen.
                        Modifier.background(colors.canvas)
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* consume — do not dismiss via scrim */ },
                )
                .nestedScroll(nestedScrollConnection)
                .pointerInput(state, screenHeightPx, expandedTopInsetPx) {
                    detectVerticalDragGestures(
                        onDragEnd = { settleFromDrag() },
                        onDragCancel = { settleFromDrag() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            applyDrag(dragAmount)
                        },
                    )
                },
        ) {

            if (isPeekVisual) {
                Box(
                    modifier = Modifier
                        .padding(
                            top = GrooveTheme.spacing.cardPadding * 2,
                            start = GrooveTheme.spacing.cardPadding,
                            end = GrooveTheme.spacing.cardPadding,
                            bottom = 0.dp
                        )
                ){
                    Text(
                        text = "Swipe up here or swipe up again to show song details",
                        style = typography.body.toTextStyle(),
                        color = colors.onSurface.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = M_PADDING)
                            .padding(bottom = M_PADDING + navBarsInset),
                    )
                }
            } else {
                SongDetailsView(
                    song = song,
                    listState = listState,
                    contentPadding = PaddingValues(
                        top = detailsTopPadding,
                        bottom = detailsBottomPadding,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
