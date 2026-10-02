package com.aethelworks.grooveplayer.presentation.player.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import android.provider.Settings
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.presentation.common.GlassWhite
import com.aethelworks.grooveplayer.presentation.common.GrooveHairline
import com.aethelworks.grooveplayer.presentation.common.GrooveSheet
import com.aethelworks.grooveplayer.presentation.common.Hairline
import com.aethelworks.grooveplayer.presentation.common.MediaArtwork
import com.aethelworks.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelworks.grooveplayer.presentation.common.Overline
import com.aethelworks.grooveplayer.presentation.common.SongAvailabilityBadge
import com.aethelworks.grooveplayer.presentation.common.rememberSongAvailabilityMark
import com.aethelworks.grooveplayer.presentation.equalizer.ui.EqualizerControlsComponent
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.icons.XChevronUp
import com.aethelworks.grooveplayer.utils.theme.icons.XMusic
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import kotlinx.coroutines.launch

/**
 * "Up next" peek row under the transport controls. Shows what plays next; tapping opens the queue sheet.
 * Renders nothing when there is no next song.
 */
@Composable
fun PhoneUpNextPeekRow(
    nextSong: Song?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nextSong == null) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GrooveTheme.radii.card))
            .background(GrooveTheme.colors.surface)
            .clickable(onClickLabel = stringResource(R.string.cd_open_queue), onClick = onClick)
            .padding(horizontal = S_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaArtwork(
            url = nextSong.artworkUrl,
            kind = MediaArtworkKind.SONG,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            cornerRadius = 6.dp,
        )
        Spacer(Modifier.width(S_PADDING))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.player_up_next),
                style = MaterialTheme.typography.labelSmall,
                color = GrooveTheme.colors.muted,
            )
            // Animate when the next song changes (skip, reorder, shuffle toggle).
            AnimatedContent(
                targetState = nextSong,
                contentKey = { it.id },
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "UpNextTitle",
            ) { next ->
                Text(
                    text = next.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(XChevronUp, contentDescription = stringResource(R.string.cd_open_queue), tint = GrooveTheme.colors.muted)
    }
}

/**
 * Phone queue bottom sheet. Standard Material3 sheet: rests half-open and can drag to full.
 * "Now playing" stays pinned; "Up next" supports tap-to-jump, drag-handle reorder and swipe-to-remove with Undo.
 * Indices passed to callbacks are absolute positions in [queue].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneQueueSheet(
    currentSong: Song?,
    queue: List<Song>,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onRestore: (index: Int, song: Song) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
    val upNextStart = currentIndex + 1 // 0 when nothing is playing from this queue

    // Local copy so reorder/remove feel instant; re-synced from the player when not dragging.
    val upNext = remember { mutableStateListOf<QueueEntry>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragFrom by remember { mutableIntStateOf(-1) }

    LaunchedEffect(queue, upNextStart) {
        if (draggingId == null) {
            upNext.clear()
            upNext.addAll(queue.drop(upNextStart).toQueueEntries())
        }
    }

    val listState = rememberLazyListState()
    val latestUpNextStart by rememberUpdatedState(upNextStart)
    val latestOnMove by rememberUpdatedState(onMove)
    val reduceMotion = rememberReducedMotion()

    GrooveSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        Box(modifier = Modifier.fillMaxHeight().navigationBarsPadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.cd_queue),
                        color = GlassWhite,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            text = queue.size.toString(),
                            color = GlassWhite.copy(alpha = 0.55f),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
                Hairline()

                if (currentSong != null) {
                    Overline(
                        text = stringResource(R.string.player_now_playing),
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                    )
                    QueueSongRow(
                        song = currentSong,
                        isNowPlaying = true,
                        playing = isPlaying,
                        reduceMotion = reduceMotion,
                        showBars = true,
                        indexLabel = null,
                        onClick = null,
                        handle = null,
                        dragging = false,
                    )
                    Hairline()
                }

                Overline(
                    text = stringResource(R.string.player_up_next),
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                )
                if (upNext.isEmpty()) {
                    Text(
                        text = stringResource(R.string.player_nothing_up_next),
                        color = GlassWhite.copy(alpha = 0.55f),
                        fontFamily = PoppinsFontFamily,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    itemsIndexed(upNext, key = { _, entry -> entry.key }) { localIndex, entry ->
                        val song = entry.song
                        val isDragging = draggingId == entry.key
                        val dismissState = rememberSwipeToDismissBoxState()
                        var removed by remember { mutableStateOf(false) }
                        val removedMessage = stringResource(R.string.player_removed, song.title)
                        val undoLabel = stringResource(R.string.action_undo)

                        LaunchedEffect(dismissState.currentValue) {
                            if (!removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                removed = true
                                val absolute = upNextStart + localIndex
                                upNext.remove(entry)
                                onRemove(absolute)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = removedMessage,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRestore(absolute, song)
                                    }
                                }
                            }
                        }

                        val itemModifier = if (isDragging) {
                            Modifier
                                .zIndex(1f)
                                .graphicsLayer { translationY = dragOffset }
                        } else {
                            Modifier.animateItem()
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = itemModifier.fillMaxWidth(),
                            enableDismissFromStartToEnd = false,
                            gesturesEnabled = draggingId == null,
                            backgroundContent = {
                                val revealed =
                                    dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                                if (revealed) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        OutlinedDeleteIcon()
                                    }
                                }
                            },
                        ) {
                            QueueSongRow(
                                song = song,
                                isNowPlaying = false,
                                playing = isPlaying,
                                reduceMotion = reduceMotion,
                                showBars = currentSong?.id == song.id,
                                indexLabel = (localIndex + 1).toString(),
                                onClick = { onSkipTo(upNextStart + localIndex) },
                                dragging = isDragging,
                                handle = Modifier.pointerInputReorder(
                                    key = entry.key,
                                    onStart = {
                                        draggingId = entry.key
                                        dragFrom = upNext.indexOfFirst { it.key == entry.key }
                                        dragOffset = 0f
                                    },
                                    onDrag = { dy ->
                                        dragOffset += dy
                                        val visible = listState.layoutInfo.visibleItemsInfo
                                        val dragged = visible.firstOrNull { it.key == entry.key }
                                        if (dragged != null) {
                                            val center = dragged.offset + dragOffset + dragged.size / 2f
                                            val target = visible.firstOrNull {
                                                it.key != entry.key && center >= it.offset && center <= it.offset + it.size
                                            }
                                            if (target != null) {
                                                val from = upNext.indexOfFirst { it.key == entry.key }
                                                val to = upNext.indexOfFirst { it.key == target.key }
                                                if (from >= 0 && to >= 0) {
                                                    upNext.add(to, upNext.removeAt(from))
                                                    dragOffset += if (to > from) -target.size else target.size
                                                }
                                            }
                                        }
                                    },
                                    onEnd = {
                                        val to = upNext.indexOfFirst { it.key == entry.key }
                                        val from = dragFrom
                                        draggingId = null
                                        dragOffset = 0f
                                        dragFrom = -1
                                        if (from >= 0 && to >= 0 && from != to) {
                                            latestOnMove(latestUpNextStart + from, latestUpNextStart + to)
                                        }
                                    },
                                ),
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF161616),
                    contentColor = GlassWhite,
                    actionColor = GlassWhite,
                )
            }
        }
    }
}

/**
 * Phone equalizer bottom sheet: fixed ~70% height (does not drag to full screen).
 * Content is the Tablet EQ panel ([com.aethelworks.grooveplayer.presentation.equalizer.ui.EqualizerControlsComponent]) in its Phone-sheet variant, so the logic is shared.
 * Band drags lock the sheet so the gesture stays on the slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEqualizerSheet(
    onDismiss: () -> Unit,
) {
    val sliderDragging = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            !sliderDragging.value || target == SheetValue.Expanded
        },
    )
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    // Drag handle sits above this content; together they land near 70% and cannot grow to full screen.
    val contentHeight = (screenHeight * 0.70f - 48.dp).coerceAtLeast(240.dp)
    GrooveSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        EqualizerControlsComponent(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .navigationBarsPadding(),
            isPhoneSheet = true,
            onSliderDragChange = { sliderDragging.value = it },
        )
    }
}

/** Stable LazyColumn key even if the same song appears twice in the queue. */
internal data class QueueEntry(val key: String, val song: Song)

internal fun List<Song>.toQueueEntries(): List<QueueEntry> {
    val seen = HashMap<String, Int>()
    return map { song ->
        val n = seen.getOrElse(song.id) { 0 }
        seen[song.id] = n + 1
        QueueEntry(key = "${song.id}#$n", song = song)
    }
}

@Composable
private fun QueueSongRow(
    song: Song,
    isNowPlaying: Boolean,
    playing: Boolean,
    reduceMotion: Boolean,
    showBars: Boolean,
    indexLabel: String?,
    onClick: (() -> Unit)?,
    handle: Modifier?,
    dragging: Boolean,
) {
    val availability = rememberSongAvailabilityMark(song)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val highlight = pressed || dragging
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isNowPlaying) 64.dp else 56.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (highlight) GlassWhite.copy(alpha = 0.06f) else Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = onClick,
                            )
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showBars) {
                        QueueBars(playing = playing && !reduceMotion)
                    } else if (indexLabel != null) {
                        Text(
                            text = indexLabel,
                            color = GlassWhite.copy(alpha = 0.40f),
                            fontFamily = PoppinsFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.size(40.dp)) {
                    QueueArtwork(
                        url = song.artworkUrl,
                        contentDescription = stringResource(R.string.cd_song_by_artist, song.title, song.artist),
                    )
                    if (availability != null) {
                        SongAvailabilityBadge(
                            mark = availability,
                            iconSize = 14.dp,
                            modifier = Modifier.align(Alignment.BottomEnd),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = GlassWhite,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = if (isNowPlaying) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (song.artist.isNotEmpty()) {
                        Text(
                            text = song.artist,
                            color = GlassWhite.copy(alpha = 0.55f),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (handle != null) {
                QueueGrip(
                    active = dragging,
                    modifier = handle.size(48.dp),
                )
            } else if (!isNowPlaying) {
                Spacer(Modifier.size(48.dp))
            }
        }
        if (!isNowPlaying) {
            Box(
                modifier = Modifier
                    .padding(start = 108.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GrooveHairline.color),
            )
        }
    }
}

@Composable
private fun QueueArtwork(
    url: String?,
    contentDescription: String?,
) {
    var failed by remember(url) { mutableStateOf(url.isNullOrBlank()) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.Center,
    ) {
        if (!failed && !url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true },
            )
        }
        if (failed) {
            Icon(
                imageVector = XMusic,
                contentDescription = contentDescription,
                tint = GlassWhite.copy(alpha = 0.40f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun QueueBars(playing: Boolean) {
    val transition = rememberInfiniteTransition(label = "queueBars")
    Row(
        modifier = Modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(0, 120, 240).forEach { delayMs ->
            QueueBar(playing = playing, delayMs = delayMs, transition = transition)
        }
    }
}

@Composable
private fun QueueBar(
    playing: Boolean,
    delayMs: Int,
    transition: InfiniteTransition,
) {
    val animated by transition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 480,
                delayMillis = delayMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar$delayMs",
    )
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(if (playing) animated.dp else 8.dp)
            .background(GlassWhite, RoundedCornerShape(1.5.dp)),
    )
}

@Composable
private fun QueueGrip(active: Boolean, modifier: Modifier = Modifier) {
    val tint = if (active) GlassWhite else GlassWhite.copy(alpha = 0.40f)
    val description = stringResource(R.string.cd_drag_to_reorder)
    Column(
        modifier = modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 1.5.dp)
                        .background(tint, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}

@Composable
private fun OutlinedDeleteIcon() {
    val label = stringResource(R.string.action_remove)
    Canvas(
        modifier = Modifier
            .size(22.dp)
            .semantics { contentDescription = label },
    ) {
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        drawLine(
            color = GlassWhite,
            start = Offset(w * 0.38f, h * 0.16f),
            end = Offset(w * 0.62f, h * 0.16f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = GlassWhite,
            start = Offset(w * 0.16f, h * 0.30f),
            end = Offset(w * 0.84f, h * 0.30f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.24f,
                    top = h * 0.36f,
                    right = w * 0.76f,
                    bottom = h * 0.88f,
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                ),
            )
        }
        drawPath(body, GlassWhite, style = stroke)
        drawLine(
            color = GlassWhite,
            start = Offset(w * 0.40f, h * 0.46f),
            end = Offset(w * 0.40f, h * 0.76f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = GlassWhite,
            start = Offset(w * 0.60f, h * 0.46f),
            end = Offset(w * 0.60f, h * 0.76f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val duration = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val transition = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        }.getOrDefault(1f)
        duration == 0f || transition == 0f
    }
}

internal fun Modifier.pointerInputReorder(
    key: Any,
    onStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(key) {
        detectDragGestures(
            onDragStart = { onStart() },
            onDrag = { change, amount ->
                change.consume()
                onDrag(amount.y)
            },
            onDragEnd = { onEnd() },
            onDragCancel = { onEnd() },
        )
    }
)
