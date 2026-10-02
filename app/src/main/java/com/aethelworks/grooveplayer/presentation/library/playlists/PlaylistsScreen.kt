package com.aethelworks.grooveplayer.presentation.library.playlists

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.domain.model.M3uImportResult
import com.aethelworks.grooveplayer.domain.model.Playlist
import com.aethelworks.grooveplayer.domain.playlist.PlaylistNames
import com.aethelworks.grooveplayer.presentation.common.GrooveMutedText
import com.aethelworks.grooveplayer.presentation.common.PlaylistListItem
import com.aethelworks.grooveplayer.presentation.common.SongListItemDefaults
import com.aethelworks.grooveplayer.presentation.common.SongListOverflowIcon
import com.aethelworks.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.theme.icons.XListMusic
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

private val PlaylistTints = listOf(
    Color(0xFF5C6BC0),
    Color(0xFF26A69A),
    Color(0xFFEF6C00),
    Color(0xFF8E24AA),
    Color(0xFF00897B),
    Color(0xFF3949AB),
)

@Composable
fun PlaylistsScreen(
    onNavigateBack: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    var createOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var pendingExport by remember { mutableStateOf<Playlist?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importFrom(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri: Uri? ->
        val playlist = pendingExport
        pendingExport = null
        if (uri != null && playlist != null) viewModel.exportTo(playlist, uri)
    }

    GrooveScreen(
        title = stringResource(R.string.home_playlists),
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
        actions = {
            TextButton(onClick = { createOpen = true }) {
                Text(stringResource(R.string.action_new), color = GrooveTheme.colors.onSurface)
            }
            TextButton(
                onClick = {
                    importLauncher.launch(
                        arrayOf(
                            "audio/x-mpegurl",
                            "audio/mpegurl",
                            "application/vnd.apple.mpegurl",
                            "application/x-mpegurl",
                            "text/plain",
                            "*/*",
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.action_import), color = GrooveTheme.colors.onSurface)
            }
        },
    ) {
        when {
            isLoading && playlists.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GrooveTheme.colors.onSurface)
                }
            }
            playlists.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GrooveMutedText(stringResource(R.string.home_no_playlists))
                        GrooveTinySpacer()
                        GrooveMutedText(stringResource(R.string.playlist_create_hint))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = M_PADDING,
                        end = M_PADDING,
                        top = topBarContentInset() + M_PADDING,
                        bottom = M_PADDING + grooveBottomContentInset(
                            includeMiniPlayer = rememberClearMiniPlayer(),
                        ),
                    ),
                    verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
                ) {
                    if (!message.isNullOrBlank()) {
                        item {
                            GrooveMutedText(message.orEmpty())
                        }
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist.id) },
                            onRename = { renameTarget = playlist },
                            onExport = {
                                pendingExport = playlist
                                exportLauncher.launch(PlaylistNames.fileName(playlist.name))
                            },
                            onDelete = { deleteTarget = playlist },
                        )
                    }
                }
            }
        }
    }

    if (createOpen) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_new),
            confirmLabel = stringResource(R.string.action_create),
            initialName = "",
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                viewModel.create(name) { id ->
                    createOpen = false
                    onOpenPlaylist(id)
                }
            },
        )
    }
    renameTarget?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.action_save),
            initialName = playlist.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.rename(playlist.id, name)
                renameTarget = null
            },
        )
    }
    deleteTarget?.let { playlist ->
        ConfirmPlaylistDialog(
            title = stringResource(R.string.playlist_delete_title),
            body = stringResource(R.string.playlist_delete_body, playlist.name),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.delete(playlist.id)
                deleteTarget = null
            },
        )
    }
    importResult?.let { result ->
        val success = result as? M3uImportResult.Success
        val importFolder = LocalLibraryImport.current.pickFolder
        AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            containerColor = GrooveTheme.colors.surface,
            titleContentColor = GrooveTheme.colors.onSurface,
            textContentColor = SoftWhite.copy(alpha = 0.85f),
            title = { Text(if (success != null) stringResource(R.string.playlist_imported) else stringResource(R.string.playlist_import_failed)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(importSummary(result))
                    if (success != null && success.missingLocations.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.dismissImportResult()
                                importFolder()
                            },
                        ) {
                            Text(stringResource(R.string.library_import_folder), color = SoftWhite)
                        }
                    }
                }
            },
            confirmButton = {
                val playlistId = success?.playlistId
                if (playlistId != null) {
                    TextButton(
                        onClick = {
                            viewModel.dismissImportResult()
                            onOpenPlaylist(playlistId)
                        },
                    ) {
                        Text(stringResource(R.string.action_open), color = SoftWhite)
                    }
                } else {
                    TextButton(onClick = viewModel::dismissImportResult) {
                        Text(stringResource(R.string.action_ok), color = SoftWhite)
                    }
                }
            },
            dismissButton = if (success != null) {
                {
                    TextButton(onClick = viewModel::dismissImportResult) {
                        Text(stringResource(R.string.action_done), color = SoftWhite.copy(alpha = 0.65f))
                    }
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val trackLabel = if (playlist.trackCount == 1) {
        stringResource(R.string.count_tracks_one)
    } else {
        stringResource(R.string.count_tracks, playlist.trackCount)
    }
    PlaylistListItem(
        title = playlist.name,
        subtitle = trackLabel,
        onClick = onClick,
        icon = { PlaylistSwatch(playlist.name) },
        menu = {
            SongListOverflowIcon(
                contentDescription = stringResource(R.string.cd_playlist_options),
                onClick = { menuOpen = true },
                dropdown = {
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = {
                                menuOpen = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_export_m3u)) },
                            onClick = {
                                menuOpen = false
                                onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                },
            )
        },
    )
}

@Composable
private fun PlaylistSwatch(name: String) {
    val tint = PlaylistTints[(name.hashCode() and Int.MAX_VALUE) % PlaylistTints.size]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.95f), tint.copy(alpha = 0.45f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = XListMusic,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

@Composable
internal fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val error = PlaylistNames.validationError(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrooveTheme.colors.surface,
        titleContentColor = GrooveTheme.colors.onSurface,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_label)) },
                singleLine = true,
                isError = name.isNotBlank() && error != null,
                supportingText = {
                    if (name.isNotBlank() && error != null) Text(error)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = error == null,
            ) {
                Text(confirmLabel, color = SoftWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}

@Composable
internal fun ConfirmPlaylistDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrooveTheme.colors.surface,
        titleContentColor = GrooveTheme.colors.onSurface,
        textContentColor = SoftWhite.copy(alpha = 0.85f),
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = SoftWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}

@Composable
private fun importSummary(result: M3uImportResult): String = when (result) {
    is M3uImportResult.Failure -> result.message
    is M3uImportResult.Success -> {
        val missing = result.missingLocations.size
        val imported = stringResource(
            R.string.playlist_imported_tracks,
            result.importedCount,
            result.playlistName,
        )
        if (missing == 0) {
            imported
        } else {
            val added = if (result.playlistId == null) {
                stringResource(R.string.playlist_nothing_added)
            } else {
                imported
            }
            stringResource(R.string.playlist_not_in_library, added)
        }
    }
}
