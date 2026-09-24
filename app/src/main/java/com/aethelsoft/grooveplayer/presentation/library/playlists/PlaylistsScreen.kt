package com.aethelsoft.grooveplayer.presentation.library.playlists

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.M3uImportResult
import com.aethelsoft.grooveplayer.domain.model.Playlist
import com.aethelsoft.grooveplayer.domain.model.trackCountLabel
import com.aethelsoft.grooveplayer.domain.playlist.PlaylistNames
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XListMusic
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

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
        title = "Playlists",
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
        actions = {
            TextButton(onClick = { createOpen = true }) {
                Text("New", color = GrooveTheme.colors.onSurface)
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
                Text("Import", color = GrooveTheme.colors.onSurface)
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
                        GrooveMutedText("No playlists yet")
                        GrooveTinySpacer()
                        GrooveMutedText("Create one, or import an M3U file")
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
                    verticalArrangement = Arrangement.spacedBy(XS_PADDING),
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
            title = "New playlist",
            confirmLabel = "Create",
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
            title = "Rename playlist",
            confirmLabel = "Save",
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
            title = "Delete playlist",
            body = "Delete \"${playlist.name}\"? Songs stay in your library.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.delete(playlist.id)
                deleteTarget = null
            },
        )
    }
    importResult?.let { result ->
        val success = result as? M3uImportResult.Success
        AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            containerColor = GrooveTheme.colors.surface,
            titleContentColor = GrooveTheme.colors.onSurface,
            textContentColor = SoftWhite.copy(alpha = 0.85f),
            title = { Text(if (success != null) "Playlist imported" else "Import failed") },
            text = { Text(importSummary(result)) },
            confirmButton = {
                if (success != null) {
                    TextButton(
                        onClick = {
                            viewModel.dismissImportResult()
                            onOpenPlaylist(success.playlistId)
                        },
                    ) {
                        Text("Open", color = SoftWhite)
                    }
                } else {
                    TextButton(onClick = viewModel::dismissImportResult) {
                        Text("OK", color = SoftWhite)
                    }
                }
            },
            dismissButton = if (success != null) {
                {
                    TextButton(onClick = viewModel::dismissImportResult) {
                        Text("Done", color = SoftWhite.copy(alpha = 0.65f))
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
    GrooveSurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(S_PADDING),
            ) {
            PlaylistSwatch(playlist.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = GrooveTheme.typography.body.toTextStyle(),
                    fontWeight = FontWeight.Medium,
                    color = GrooveTheme.colors.onSurface,
                )
                GrooveTinySpacer()
                GrooveMutedText(trackCountLabel(playlist.trackCount))
            }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(XMore, contentDescription = "Playlist options", tint = SoftWhite)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Export M3U") },
                        onClick = {
                            menuOpen = false
                            onExport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSwatch(name: String) {
    val tint = PlaylistTints[(name.hashCode() and Int.MAX_VALUE) % PlaylistTints.size]
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
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
                label = { Text("Name") },
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
                Text("Cancel", color = SoftWhite.copy(alpha = 0.65f))
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
                Text("Cancel", color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}

private fun importSummary(result: M3uImportResult): String = when (result) {
    is M3uImportResult.Failure -> result.message
    is M3uImportResult.Success -> {
        val missing = result.missingLocations.size
        if (missing == 0) {
            "Imported ${result.importedCount} tracks into ${result.playlistName}."
        } else {
            val files = if (missing == 1) "1 file was" else "$missing files were"
            "Imported ${result.importedCount} tracks into ${result.playlistName}. $files not in your library."
        }
    }
}
