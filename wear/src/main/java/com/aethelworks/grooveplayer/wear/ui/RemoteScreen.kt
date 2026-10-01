package com.aethelworks.grooveplayer.wear.ui

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import com.aethelworks.grooveplayer.wear.logic.Outline
import com.aethelworks.grooveplayer.wear.logic.RemoteGesture
import com.aethelworks.grooveplayer.wear.logic.RemoteMetrics
import com.aethelworks.grooveplayer.wear.logic.RemoteUiState
import com.aethelworks.grooveplayer.wear.logic.cappedSafeInsetPx
import com.aethelworks.grooveplayer.wear.logic.centerlineOutline
import com.aethelworks.grooveplayer.wear.logic.classifyRemoteGesture
import com.aethelworks.grooveplayer.wear.logic.distanceTo
import com.aethelworks.grooveplayer.wear.logic.fractionAt
import com.aethelworks.grooveplayer.wear.logic.formatClock
import com.aethelworks.grooveplayer.wear.logic.positionForFraction
import com.aethelworks.grooveplayer.wear.logic.progressFraction
import com.aethelworks.grooveplayer.wear.logic.resolvedCornerRadiusPx
import com.aethelworks.grooveplayer.wear.logic.ringSegments
import com.aethelworks.grooveplayer.wear.logic.strokeWidthPx
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

@Composable
fun RemoteScreen(
    state: RemoteUiState,
    artwork: Painter?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val view = LocalView.current
    val screenRound = LocalConfiguration.current.isScreenRound
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val durationMs by rememberUpdatedState(state.durationMs)
    val seekEnabled by rememberUpdatedState(state.connected && state.durationMs > 0L)
    val controlsEnabled by rememberUpdatedState(state.connected)
    val playPause by rememberUpdatedState(onPlayPause)
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val seek by rememberUpdatedState(onSeek)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val minSide = min(widthPx, heightPx)
        val safeInset = uniformSafeInsetPx(
            WindowInsets.safeDrawing,
            density,
            layoutDirection,
            minSide,
        )
        val corner = resolvedCornerRadiusPx(
            widthPx = widthPx,
            heightPx = heightPx,
            reportedCornerRadiiPx = reportedCornerRadiiPx(view),
            screenRound = screenRound,
        )
        val outline = centerlineOutline(widthPx, heightPx, corner, safeInset)
        val stroke = strokeWidthPx(widthPx, heightPx)
        val shownFraction = dragFraction ?: progressFraction(state.positionMs, state.durationMs)
        val shownPosition = if (dragFraction != null) {
            positionForFraction(dragFraction!!, state.durationMs)
        } else {
            state.positionMs
        }
        val textInset = with(density) { (minSide * RemoteMetrics.TEXT_INSET_FRACTION).toDp() }
        val titleSize = with(density) { (minSide * RemoteMetrics.TITLE_FRACTION).toSp() }
        val artistSize = with(density) { (minSide * RemoteMetrics.ARTIST_FRACTION).toSp() }
        val timeSize = with(density) { (minSide * RemoteMetrics.TIME_FRACTION).toSp() }
        val mark = with(density) { (minSide * RemoteMetrics.MARK_FRACTION).toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(outline, widthPx, heightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = down.position
                        var end = start
                        val span = min(size.width, size.height).toFloat()
                        val onRing = controlsEnabled &&
                            outline.distanceTo(start.x, start.y) <= span * RemoteMetrics.HIT_FRACTION
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                end = change.position
                                val seeking = onRing && seekEnabled
                                if (seeking) {
                                    dragFraction = outline.fractionAt(end.x, end.y)
                                    change.consume()
                                } else if (
                                    abs(end.x - start.x) > abs(end.y - start.y) &&
                                    abs(end.x - start.x) > span * RemoteMetrics.TAP_FRACTION
                                ) {
                                    change.consume()
                                }
                                if (!change.pressed) break
                            }
                            when (
                                val gesture = classifyRemoteGesture(
                                    downX = start.x,
                                    downY = start.y,
                                    upX = end.x,
                                    upY = end.y,
                                    outline = outline,
                                    minSidePx = span,
                                    seekEnabled = seekEnabled,
                                    enabled = controlsEnabled,
                                )
                            ) {
                                RemoteGesture.PlayPause -> playPause()
                                RemoteGesture.Next -> next()
                                RemoteGesture.Previous -> previous()
                                is RemoteGesture.Seek -> seek(positionForFraction(gesture.fraction, durationMs))
                                RemoteGesture.Ignore -> Unit
                            }
                        } finally {
                            dragFraction = null
                        }
                    }
                },
        ) {
            if (artwork != null) {
                Image(
                    painter = artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (state.connected) 0.2f else 0.55f),
                            Color.Black.copy(alpha = if (state.connected) 0.62f else 0.88f),
                        ),
                        radius = size.minDimension * 0.72f,
                    ),
                )
                val path = ringPath(outline)
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = if (state.connected) 0.38f else 0.18f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                if (state.connected && shownFraction > 0f) {
                    val measure = PathMeasure()
                    measure.setPath(path, false)
                    val progress = Path()
                    measure.getSegment(0f, measure.length * shownFraction.coerceIn(0f, 1f), progress, true)
                    drawPath(
                        path = progress,
                        color = Color(0xFFFFC46B),
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = textInset),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.showTransport) {
                    Canvas(Modifier.size(mark)) {
                        if (state.isPlaying) drawPauseMark() else drawPlayMark()
                    }
                }
                BasicText(
                    text = state.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = titleSize,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.artist.isNotBlank()) {
                    BasicText(
                        text = state.artist,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = artistSize,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.showTransport && state.durationMs > 0L) {
                    BasicText(
                        text = "${formatClock(shownPosition)} / ${formatClock(state.durationMs)}",
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = timeSize,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun uniformSafeInsetPx(
    insets: WindowInsets,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: LayoutDirection,
    minSidePx: Float,
): Float {
    val left = insets.getLeft(density, layoutDirection).toFloat()
    val top = insets.getTop(density).toFloat()
    val right = insets.getRight(density, layoutDirection).toFloat()
    val bottom = insets.getBottom(density).toFloat()
    return cappedSafeInsetPx(maxOf(left, top, right, bottom), minSidePx)
}

internal fun reportedCornerRadiiPx(view: View): List<Float> {
    if (Build.VERSION.SDK_INT < 31) return emptyList()
    val insets = view.rootWindowInsets ?: return emptyList()
    return listOf(
        RoundedCorner.POSITION_TOP_LEFT,
        RoundedCorner.POSITION_TOP_RIGHT,
        RoundedCorner.POSITION_BOTTOM_RIGHT,
        RoundedCorner.POSITION_BOTTOM_LEFT,
    ).mapNotNull { position -> insets.getRoundedCorner(position)?.radius?.toFloat() }
}

private fun ringPath(outline: Outline): Path {
    val path = Path()
    val segments = ringSegments(outline)
    val first = segments.firstOrNull() ?: return path
    val start = first.pointAt(0f)
    path.moveTo(start.first, start.second)
    for (segment in segments) {
        when (segment) {
            is com.aethelworks.grooveplayer.wear.logic.RingSegment.Line -> path.lineTo(segment.x1, segment.y1)
            is com.aethelworks.grooveplayer.wear.logic.RingSegment.Arc -> {
                path.arcTo(
                    rect = Rect(
                        segment.cx - segment.radius,
                        segment.cy - segment.radius,
                        segment.cx + segment.radius,
                        segment.cy + segment.radius,
                    ),
                    startAngleDegrees = segment.startAngle * 180f / PI.toFloat(),
                    sweepAngleDegrees = segment.sweep * 180f / PI.toFloat(),
                    forceMoveTo = false,
                )
            }
        }
    }
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlayMark() {
    val path = Path().apply {
        moveTo(size.width * 0.30f, size.height * 0.16f)
        lineTo(size.width * 0.82f, size.height * 0.50f)
        lineTo(size.width * 0.30f, size.height * 0.84f)
        close()
    }
    drawPath(path, Color.White)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPauseMark() {
    val radius = CornerRadius(size.width * 0.08f, size.width * 0.08f)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(size.width * 0.16f, size.height * 0.16f),
        size = Size(size.width * 0.22f, size.height * 0.68f),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(size.width * 0.62f, size.height * 0.16f),
        size = Size(size.width * 0.22f, size.height * 0.68f),
        cornerRadius = radius,
    )
}
