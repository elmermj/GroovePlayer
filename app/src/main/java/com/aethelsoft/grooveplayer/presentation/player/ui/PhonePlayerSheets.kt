package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.animations.AudioWaveAnimation
import com.aethelsoft.grooveplayer.utils.theme.icons.XChevronUp
import com.aethelsoft.grooveplayer.utils.theme.icons.XGripVertical
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import kotlinx.coroutines.launch

/**
 * Subtle dark gradient drawn behind the phone queue and EQ sheets.
 * The sheets themselves use a transparent scrim so this gradient shows through the dialog window.
 */
@Composable
fun PlayerSheetGradientScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to Color.Black.copy(alpha = 0.22f),
                    1f to Color.Black.copy(alpha = 0.55f),
                )
            )
    )
}

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
            .clickable(onClickLabel = "Open queue", onClick = onClick)
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
                text = "Up next",
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
        Icon(XChevronUp, contentDescription = "Open queue", tint = GrooveTheme.colors.muted)
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

    val sheetColor = GrooveTheme.colors.edgeGradient
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
        containerColor = sheetColor,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = GrooveTheme.radii.card, topEnd = GrooveTheme.radii.card),
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = M_PADDING, vertical = S_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            text = queue.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = GrooveTheme.colors.muted,
                        )
                    }
                }

                if (currentSong != null) {
                    SectionLabel("Now playing")
                    QueueSongRow(
                        song = currentSong,
                        isNowPlaying = true,
                        onClick = null,
                        handle = null,
                        pinned = true,
                    )
                }

                SectionLabel(if (upNext.isEmpty()) "Nothing up next" else "Up next")

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    itemsIndexed(upNext, key = { _, entry -> entry.key }) { localIndex, entry ->
                        val song = entry.song
                        val isDragging = draggingId == entry.key
                        val dismissState = rememberSwipeToDismissBoxState()
                        var removed by remember { mutableStateOf(false) }

                        LaunchedEffect(dismissState.currentValue) {
                            if (!removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                removed = true
                                val absolute = upNextStart + localIndex
                                upNext.remove(entry)
                                onRemove(absolute)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Removed \"${song.title}\"",
                                        actionLabel = "Undo",
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
                                .graphicsLayer {
                                    translationY = dragOffset
                                    shadowElevation = 12f
                                }
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
                                            .background(GrooveTheme.colors.error)
                                            .padding(horizontal = M_PADDING),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Text(
                                            "Remove",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            },
                        ) {
                            // The dismiss background is always composed behind the row. An opaque
                            // sheet color covers it at rest; a left swipe translates this row and
                            // reveals Remove. A transparent row leaves the red fill visible.
                            QueueSongRow(
                                song = song,
                                isNowPlaying = false,
                                onClick = { onSkipTo(upNextStart + localIndex) },
                                containerColor = sheetColor,
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
                    .navigationBarsPadding()
                    .padding(M_PADDING),
            )
        }
    }
}

/**
 * Phone equalizer bottom sheet: fixed ~70% height (does not drag to full screen).
 * Content is the Tablet EQ panel ([EqualizerControlsComponent]) in its Phone-sheet variant, so the logic is shared.
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
        containerColor = GrooveTheme.colors.edgeGradient,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = GrooveTheme.radii.card, topEnd = GrooveTheme.radii.card),
    ) {
        EqualizerControlsComponent(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = GrooveTheme.colors.muted,
        modifier = Modifier.padding(start = M_PADDING, end = M_PADDING, top = S_PADDING, bottom = 4.dp),
    )
}

/** Queue row styled like the unified library song row (56dp artwork, menu song typography). */
@Composable
private fun QueueSongRow(
    song: Song,
    isNowPlaying: Boolean,
    onClick: (() -> Unit)?,
    handle: Modifier?,
    pinned: Boolean = false,
    containerColor: Color = Color.Transparent,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (pinned) M_PADDING else 0.dp)
            .clip(RoundedCornerShape(if (pinned) GrooveTheme.radii.card else 0.dp))
            .background(if (pinned) GrooveTheme.colors.surface else containerColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = M_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S_PADDING),
    ) {
        Box(contentAlignment = Alignment.Center) {
            MediaArtwork(
                url = song.artworkUrl,
                kind = MediaArtworkKind.SONG,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                cornerRadius = S_PADDING,
            )
            if (isNowPlaying) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(S_PADDING))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AudioWaveAnimation(waveHeight = 18.dp, modifier = Modifier.width(40.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = GrooveTheme.typography.menuSongTitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = GrooveTheme.typography.menuSongArtist.toTextStyle(),
                color = GrooveTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val availability = com.aethelsoft.grooveplayer.presentation.common.rememberSongAvailabilityMark(song)
        if (availability != null) {
            com.aethelsoft.grooveplayer.presentation.common.SongAvailabilityBadge(
                mark = availability,
                iconSize = 16.dp,
            )
        }
        if (handle != null) {
            Box(
                modifier = handle
                    .size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(XGripVertical, contentDescription = "Drag to reorder", tint = GrooveTheme.colors.muted)
            }
        }
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
