package com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail

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
import com.aethelworks.grooveplayer.domain.model.PlaylistTrack
import com.aethelworks.grooveplayer.domain.playlist.PlaylistNames
import com.aethelworks.grooveplayer.domain.playlist.playlistQueueStart
import com.aethelworks.grooveplayer.presentation.common.GrooveActionButton
import com.aethelworks.grooveplayer.presentation.common.GrooveMutedText
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelworks.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelworks.grooveplayer.presentation.library.playlists.ConfirmPlaylistDialog
import com.aethelworks.grooveplayer.presentation.library.playlists.PlaylistNameDialog
import com.aethelworks.grooveplayer.presentation.library.playlists.PlaylistTrackList
import com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.layouts.LargeTabletPlaylistDetailLayout
import com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.layouts.PhonePlaylistDetailLayout
import com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.layouts.TabletPlaylistDetailLayout
import com.aethelworks.grooveplayer.presentation.player.PlayerViewModel
import com.aethelworks.grooveplayer.utils.DeviceType
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.XS_PADDING
import com.aethelworks.grooveplayer.utils.rememberDeviceType
import com.aethelworks.grooveplayer.utils.theme.icons.XMore
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

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

    val deviceType = rememberDeviceType()

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
        val layoutPadding = PaddingValues.Zero
        val horizontalPadding = M_PADDING

        Box(modifier = Modifier.fillMaxSize()) {
            when (deviceType) {
                DeviceType.PHONE -> {
                    PhonePlaylistDetailLayout(
                        playlist = playlist,
                        tracks = tracks,
                        isLoading = isLoading,
                        message = message,
                        onAddTracks = onAddTracks,
                        viewModel = viewModel,
                        playerViewModel = playerViewModel
                    )
                }

                DeviceType.TABLET -> {
                    TabletPlaylistDetailLayout(
                        playlist = playlist,
                        tracks = tracks,
                        isLoading = isLoading,
                        message = message,
                        onAddTracks = onAddTracks,
                        viewModel = viewModel,
                        playerViewModel = playerViewModel
                    )
                }

                DeviceType.LARGE_TABLET -> {
                    LargeTabletPlaylistDetailLayout(
                        playlist = playlist,
                        tracks = tracks,
                        isLoading = isLoading,
                        message = message,
                        onAddTracks = onAddTracks,
                        viewModel = viewModel,
                        playerViewModel = playerViewModel
                    )
                }
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

fun playPlaylist(
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
