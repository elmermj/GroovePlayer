package com.aethelworks.grooveplayer.presentation.player.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.presentation.common.SongAvailabilityBadge
import com.aethelworks.grooveplayer.presentation.common.SongListItem
import com.aethelworks.grooveplayer.presentation.common.SongListItemDefaults
import com.aethelworks.grooveplayer.presentation.common.SongListSlots
import com.aethelworks.grooveplayer.presentation.common.rememberSongAvailabilityMark
import com.aethelworks.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelworks.grooveplayer.presentation.library.importing.rememberTrackPresence
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.HighlightPrimary
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
            verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
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
                            reorderable = onMove != null,
                        )
                    }
                } else {
                    SideQueueRow(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = { onItemClick(index) },
                        handle = handle,
                        reorderable = onMove != null,
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
    reorderable: Boolean,
    modifier: Modifier = Modifier,
) {
    val presence = rememberTrackPresence(song)
    val import = LocalLibraryImport.current
    val availability = rememberSongAvailabilityMark(song)
    val slots = when {
        handle != null -> SongListSlots.UpNext
        reorderable -> SongListSlots.NowPlaying
        else -> SongListSlots.None
    }
    SongListItem(
        title = song.title,
        artist = if (presence.playable) {
            song.artist
        } else {
            stringResource(R.string.library_unavailable, song.artist)
        },
        artworkUrl = song.artworkUrl,
        artworkContentDescription = stringResource(R.string.cd_song_by_artist, song.title, song.artist),
        modifier = modifier,
        slots = slots,
        highlighted = isPlaying,
        contentAlpha = if (presence.playable) 1f else 0.45f,
        onClick = if (presence.playable) onClick else null,
        dragHandleModifier = handle ?: Modifier,
        artworkBadge = if (availability != null) {
            {
                SongAvailabilityBadge(
                    mark = availability,
                    iconSize = 16.dp,
                )
            }
        } else {
            null
        },
        extraTrailing = if (presence.canRestore) {
            {
                TextButton(onClick = { import.restoreSong(song) }) {
                    Text(stringResource(R.string.action_restore), color = HighlightPrimary)
                }
            }
        } else {
            null
        },
    )
}
