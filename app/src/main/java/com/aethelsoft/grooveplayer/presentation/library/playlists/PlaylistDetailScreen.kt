package com.aethelsoft.grooveplayer.presentation.library.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.PlaylistTrack
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.domain.playlist.playlistQueueStart
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onAddTracks: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val playlist by viewModel.playlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val playerViewModel = rememberPlayerViewModel()
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    val title = playlist?.playlist?.name ?: "Playlist"
    val tracks = playlist?.tracks.orEmpty()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }

    GrooveScreen(
        title = title,
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
        actions = {
            TextButton(onClick = onAddTracks) {
                Text("Add", color = GrooveTheme.colors.onSurface)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(XMore, contentDescription = "Playlist options", tint = GrooveTheme.colors.onSurface)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            renameOpen = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Export M3U") },
                        onClick = {
                            menuOpen = false
                            exportLauncher.launch(PlaylistNames.fileName(title))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuOpen = false
                            deleteOpen = true
                        },
                    )
                }
            }
        },
    ) {
        when {
            isLoading && playlist == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GrooveTheme.colors.onSurface)
                }
            }
            playlist == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GrooveMutedText("Playlist not found")
                }
            }
            tracks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GrooveMutedText("No tracks yet")
                        GrooveTinySpacer()
                        TextButton(onClick = onAddTracks) {
                            Text("Add tracks", color = SoftWhite)
                        }
                    }
                }
            }
            else -> {
                PlaylistTrackList(
                    tracks = tracks,
                    onPlay = { index -> playPlaylist(playerViewModel, tracks, index) },
                    onMove = viewModel::move,
                    onRemove = viewModel::removeTrack,
                    header = {
                        Column(verticalArrangement = Arrangement.spacedBy(XS_PADDING)) {
                            GrooveActionButton(
                                label = "Play",
                                onClick = { playPlaylist(playerViewModel, tracks, tappedIndex = null) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (!message.isNullOrBlank()) {
                                GrooveMutedText(
                                    text = message.orEmpty(),
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    if (renameOpen) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initialName = title,
            onDismiss = { renameOpen = false },
            onConfirm = { name ->
                viewModel.rename(name)
                renameOpen = false
            },
        )
    }
    if (deleteOpen) {
        ConfirmPlaylistDialog(
            title = "Delete playlist",
            body = "Delete \"$title\"? Songs stay in your library.",
            confirmLabel = "Delete",
            onDismiss = { deleteOpen = false },
            onConfirm = {
                deleteOpen = false
                viewModel.delete(onNavigateBack)
            },
        )
    }
}

private fun playPlaylist(
    playerViewModel: PlayerViewModel,
    tracks: List<PlaylistTrack>,
    tappedIndex: Int?,
) {
    val playable = tracks.filter { it.available }
    val start = playlistQueueStart(
        ids = tracks.map { it.song.id },
        tappedIndex = tappedIndex,
        playableIds = playable.map { it.song.id },
    )
    if (start < 0) return
    playerViewModel.setQueue(playable.map { it.song }, start)
}
