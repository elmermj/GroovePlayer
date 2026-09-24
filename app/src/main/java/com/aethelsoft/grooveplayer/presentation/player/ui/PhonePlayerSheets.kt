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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import kotlinx.coroutines.launch

/** Same subtle scrim the Phone FullPlayer overlays already use. */
private val PhoneSheetScrim = Color.Black.copy(alpha = 0.5f)
private val PhoneSheetContainer = Color(0xFF121212)

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
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
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
                color = SoftWhite,
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
        Icon(XChevronUp, contentDescription = null, tint = SoftWhite)
    }
}

/**
 * Phone queue bottom sheet. Opens partially expanded and drags up to full screen.
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = PhoneSheetScrim,
        containerColor = PhoneSheetContainer,
        contentColor = Color.White,
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = M_PADDING, vertical = S_PADDING),
                )

                if (currentSong != null) {
                    SectionLabel("Now playing")
                    QueueSongRow(
                        song = currentSong,
                        isNowPlaying = true,
                        onClick = null,
                        handle = null,
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

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
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
                            modifier = itemModifier,
                            enableDismissFromStartToEnd = false,
                            gesturesEnabled = draggingId == null,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFB3261E))
                                        .padding(horizontal = M_PADDING),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text("Remove", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                }
                            },
                        ) {
                            QueueSongRow(
                                song = song,
                                isNowPlaying = false,
                                onClick = { onSkipTo(upNextStart + localIndex) },
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEqualizerSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = PhoneSheetScrim,
        containerColor = PhoneSheetContainer,
        contentColor = Color.White,
    ) {
        EqualizerControlsComponent(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            isPhoneSheet = true,
        )
    }
}

/** Stable LazyColumn key even if the same song appears twice in the queue. */
private data class QueueEntry(val key: String, val song: Song)

private fun List<Song>.toQueueEntries(): List<QueueEntry> {
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
        color = SoftWhite,
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isNowPlaying) Color.White.copy(alpha = 0.06f) else PhoneSheetContainer)
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
                color = SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (handle != null) {
            Box(
                modifier = handle
                    .size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(XGripVertical, contentDescription = "Drag to reorder", tint = SoftWhite)
            }
        }
    }
}

private fun Modifier.pointerInputReorder(
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
