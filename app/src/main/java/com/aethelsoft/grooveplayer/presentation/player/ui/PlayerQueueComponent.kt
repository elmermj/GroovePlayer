package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelsoft.grooveplayer.presentation.library.importing.rememberTrackPresence
import com.aethelsoft.grooveplayer.utils.theme.ui.HighlightPrimary
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.animations.AudioWaveAnimation
import com.aethelsoft.grooveplayer.utils.theme.icons.XGripVertical
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import kotlinx.coroutines.launch

/**
 * Tablet / large-tablet queue side panel.
 * Tap jumps to that index. Upcoming rows can be reordered with the grip and removed with a left swipe (Undo).
 * Indices passed to callbacks are positions in [queue].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueComponent(
    currentSong: Song?,
    queue: List<Song>,
    onItemClick: (index: Int) -> Unit,
    maxHeight: Dp,
    onRemove: ((index: Int) -> Unit)? = null,
    onMove: ((from: Int, to: Int) -> Unit)? = null,
    onRestore: ((index: Int, song: Song) -> Unit)? = null,
) {
    val entries = remember { mutableStateListOf<QueueEntry>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragFrom by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val latestOnMove by rememberUpdatedState(onMove)

    LaunchedEffect(queue) {
        if (draggingId == null) {
            entries.clear()
            entries.addAll(queue.toQueueEntries())
        }
    }

    Box(
        modifier = Modifier
            .heightIn(max = maxHeight)
            .width(360.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = M_PADDING)
                .fillMaxWidth()
                .heightIn(max = maxHeight),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                val song = entry.song
                val isPlaying = currentSong?.id == song.id
                val isDragging = draggingId == entry.key
                val canEdit = !isPlaying && draggingId == null

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

                val handle = if (onMove != null && !isPlaying) {
                    Modifier.pointerInputReorder(
                        key = entry.key,
                        onStart = {
                            draggingId = entry.key
                            dragFrom = entries.indexOfFirst { it.key == entry.key }
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
                                    val from = entries.indexOfFirst { it.key == entry.key }
                                    val to = entries.indexOfFirst { it.key == target.key }
                                    if (from >= 0 && to >= 0) {
                                        entries.add(to, entries.removeAt(from))
                                        dragOffset += if (to > from) -target.size else target.size
                                    }
                                }
                            }
                        },
                        onEnd = {
                            val to = entries.indexOfFirst { it.key == entry.key }
                            val from = dragFrom
                            draggingId = null
                            dragOffset = 0f
                            dragFrom = -1
                            if (from >= 0 && to >= 0 && from != to) {
                                latestOnMove?.invoke(from, to)
                            }
                        },
                    )
                } else {
                    null
                }

                if (onRemove != null) {
                    val dismissState = rememberSwipeToDismissBoxState()
                    var removed by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissState.currentValue) {
                        if (!isPlaying && !removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            removed = true
                            entries.remove(entry)
                            onRemove(index)
                            snackbarHostState.currentSnackbarData?.dismiss()
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed \"${song.title}\"",
                                    actionLabel = if (onRestore != null) "Undo" else null,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onRestore?.invoke(index, song)
                                }
                            }
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = itemModifier.fillMaxWidth(),
                        enableDismissFromStartToEnd = false,
                        gesturesEnabled = canEdit && !isPlaying,
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
                        SideQueueRow(
                            song = song,
                            isPlaying = isPlaying,
                            onClick = { onItemClick(index) },
                            handle = handle,
                            // Covers the red Remove background until the row is actually swiped.
                            containerColor = GrooveTheme.colors.canvas,
                        )
                    }
                } else {
                    SideQueueRow(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = { onItemClick(index) },
                        handle = handle,
                        modifier = itemModifier,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(S_PADDING),
        )
    }
}

@Composable
private fun SideQueueRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    handle: Modifier?,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
) {
    val presence = rememberTrackPresence(song)
    val import = LocalLibraryImport.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .alpha(if (presence.playable) 1f else 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = presence.playable, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(S_PADDING))
                Text(
                    text = song.title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(XS_PADDING / 2))
                Text(
                    text = if (presence.playable) song.artist else "${song.artist} · Unavailable",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    color = GrooveTheme.colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (presence.canRestore) {
                    TextButton(
                        onClick = { import.restoreSong(song) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Restore", color = HighlightPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(S_PADDING))
            }
            val availability = com.aethelsoft.grooveplayer.presentation.common.rememberSongAvailabilityMark(song)
            if (availability != null) {
                com.aethelsoft.grooveplayer.presentation.common.SongAvailabilityBadge(
                    mark = availability,
                    iconSize = 16.dp,
                )
            }
            MediaArtwork(
                url = song.artworkUrl,
                kind = MediaArtworkKind.SONG,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artworkUrl)
                    .size(36, 36)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                cornerRadius = 4.dp,
                contentScale = ContentScale.Crop,
            )
            if (handle != null) {
                Box(
                    modifier = handle.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        XGripVertical,
                        contentDescription = "Drag to reorder",
                        tint = GrooveTheme.colors.muted,
                    )
                }
            }
        }
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.85f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AudioWaveAnimation(
                    waveHeight = 24.dp,
                    edgeFadeWidth = 36.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
