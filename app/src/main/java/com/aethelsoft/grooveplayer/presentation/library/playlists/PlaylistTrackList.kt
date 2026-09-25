package com.aethelsoft.grooveplayer.presentation.library.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import com.aethelsoft.grooveplayer.domain.model.PlaylistTrack
import com.aethelsoft.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelsoft.grooveplayer.presentation.library.importing.rememberTrackPresence
import com.aethelsoft.grooveplayer.utils.theme.ui.HighlightPrimary
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XGripVertical
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun PlaylistTrackList(
    tracks: List<PlaylistTrack>,
    onPlay: (index: Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (entryId: Long) -> Unit,
    header: @Composable () -> Unit = {},
) {
    val entries = remember { mutableStateListOf<PlaylistTrack>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragFrom by remember { mutableIntStateOf(-1) }
    var pendingOrder by remember { mutableStateOf<List<Long>?>(null) }
    val listState = rememberLazyListState()
    val latestOnMove by rememberUpdatedState(onMove)

    LaunchedEffect(tracks, pendingOrder) {
        val pending = pendingOrder
        val incoming = tracks.map { it.entryId }
        if (pending == null) {
            if (draggingId == null) {
                entries.clear()
                entries.addAll(tracks)
            }
        } else if (incoming == pending) {
            pendingOrder = null
            entries.clear()
            entries.addAll(tracks)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = M_PADDING,
            end = M_PADDING,
            top = topBarContentInset() + M_PADDING,
            bottom = M_PADDING + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
        ),
        verticalArrangement = Arrangement.spacedBy(XS_PADDING),
    ) {
        item { header() }
        itemsIndexed(entries, key = { _, track -> track.entryId }) { index, track ->
            val isDragging = draggingId == track.entryId
            val rowModifier = if (isDragging) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer { translationY = dragOffset }
            } else {
                Modifier.animateItem()
            }
            PlaylistTrackRow(
                track = track,
                modifier = rowModifier,
                canMoveUp = index > 0 && draggingId == null,
                canMoveDown = index < entries.lastIndex && draggingId == null,
                onPlay = { onPlay(entries.indexOfFirst { it.entryId == track.entryId }) },
                onMoveUp = { latestOnMove(index, index - 1) },
                onMoveDown = { latestOnMove(index, index + 1) },
                onRemove = { onRemove(track.entryId) },
                dragHandle = Modifier.pointerInput(track.entryId) {
                    detectDragGestures(
                        onDragStart = {
                            draggingId = track.entryId
                            dragFrom = entries.indexOfFirst { it.entryId == track.entryId }
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val visible = listState.layoutInfo.visibleItemsInfo
                            val dragged = visible.firstOrNull { it.key == track.entryId }
                            if (dragged != null) {
                                val center = dragged.offset + dragOffset + dragged.size / 2f
                                val target = visible.firstOrNull {
                                    it.key != track.entryId &&
                                        center >= it.offset &&
                                        center <= it.offset + it.size
                                }
                                if (target != null) {
                                    val from = entries.indexOfFirst { it.entryId == track.entryId }
                                    val to = entries.indexOfFirst { it.entryId == target.key }
                                    if (from >= 0 && to >= 0) {
                                        entries.add(to, entries.removeAt(from))
                                        dragOffset += if (to > from) -target.size else target.size
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            finishDrag(
                                entryId = track.entryId,
                                dragFrom = dragFrom,
                                entries = entries,
                                onMove = latestOnMove,
                                onPending = { pendingOrder = it },
                                clearDrag = {
                                    draggingId = null
                                    dragOffset = 0f
                                    dragFrom = -1
                                },
                            )
                        },
                        onDragCancel = {
                            draggingId = null
                            dragOffset = 0f
                            dragFrom = -1
                            pendingOrder = null
                            entries.clear()
                            entries.addAll(tracks)
                        },
                    )
                },
            )
        }
    }
}

private fun finishDrag(
    entryId: Long,
    dragFrom: Int,
    entries: List<PlaylistTrack>,
    onMove: (Int, Int) -> Unit,
    onPending: (List<Long>) -> Unit,
    clearDrag: () -> Unit,
) {
    val to = entries.indexOfFirst { it.entryId == entryId }
    val from = dragFrom
    clearDrag()
    if (from >= 0 && to >= 0 && from != to) {
        onPending(entries.map { it.entryId })
        onMove(from, to)
    }
}

@Composable
private fun PlaylistTrackRow(
    track: PlaylistTrack,
    modifier: Modifier,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    dragHandle: Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val song = track.song
    val presence = rememberTrackPresence(song)
    val import = LocalLibraryImport.current
    val playable = track.available && presence.playable
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (playable) 1f else 0.45f)
            .clip(GrooveTheme.radii.cardShape)
            .background(GrooveTheme.colors.surface)
            .padding(vertical = XS_PADDING, horizontal = S_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S_PADDING),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = {
                    if (!playable) return@clickable
                    onPlay()
                }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            MediaArtwork(
                url = song.artworkUrl,
                kind = MediaArtworkKind.SONG,
                contentDescription = song.title,
                modifier = Modifier.size(48.dp),
                cornerRadius = 12.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = GrooveTheme.typography.body.toTextStyle(),
                    color = GrooveTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (playable) song.artist else "${song.artist} · Unavailable",
                    style = GrooveTheme.typography.menuSongArtist.toTextStyle(),
                    color = SoftWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (presence.canRestore) {
            TextButton(onClick = { import.restoreSong(song) }) {
                Text("Restore", color = HighlightPrimary)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(XMore, contentDescription = "Track options", tint = SoftWhite)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Move up") },
                    enabled = canMoveUp,
                    onClick = {
                        menuOpen = false
                        onMoveUp()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Move down") },
                    enabled = canMoveDown,
                    onClick = {
                        menuOpen = false
                        onMoveDown()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
        Box(
            modifier = dragHandle.size(48.dp),
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
