package com.aethelworks.grooveplayer.presentation.library.playlists

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.aethelworks.grooveplayer.domain.model.PlaylistTrack
import com.aethelworks.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelworks.grooveplayer.presentation.library.importing.rememberTrackPresence
import com.aethelworks.grooveplayer.utils.theme.ui.HighlightPrimary
import com.aethelworks.grooveplayer.presentation.common.SongListItem
import com.aethelworks.grooveplayer.presentation.common.SongListItemDefaults
import com.aethelworks.grooveplayer.presentation.common.SongListOverflowIcon
import com.aethelworks.grooveplayer.presentation.common.SongListSlots
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.utils.M_PADDING

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
        verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
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
    SongListItem(
        title = song.title,
        artist = if (playable) song.artist else stringResource(R.string.library_unavailable, song.artist),
        artworkUrl = song.artworkUrl,
        artworkContentDescription = stringResource(R.string.cd_song_by_artist, song.title, song.artist),
        modifier = modifier,
        slots = SongListSlots.Playlist,
        contentAlpha = if (playable) 1f else 0.45f,
        onClick = if (playable) onPlay else null,
        dragHandleModifier = dragHandle,
        extraTrailing = if (presence.canRestore) {
            {
                TextButton(onClick = { import.restoreSong(song) }) {
                    Text(stringResource(R.string.action_restore), color = HighlightPrimary)
                }
            }
        } else {
            null
        },
        menu = {
            SongListOverflowIcon(
                contentDescription = stringResource(R.string.cd_track_options),
                onClick = { menuOpen = true },
                dropdown = {
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_move_up)) },
                            enabled = canMoveUp,
                            onClick = {
                                menuOpen = false
                                onMoveUp()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_move_down)) },
                            enabled = canMoveDown,
                            onClick = {
                                menuOpen = false
                                onMoveDown()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove)) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                },
            )
        },
    )
}
