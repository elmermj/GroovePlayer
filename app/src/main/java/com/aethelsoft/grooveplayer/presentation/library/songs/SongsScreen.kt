package com.aethelsoft.grooveplayer.presentation.library.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.songs.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.rememberAudioPermissionState
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SongsViewModel = hiltViewModel()
) {

    val (hasPermission, requestPermission) = rememberAudioPermissionState()
    val uiState by viewModel.uiState.collectAsState()
    val deviceType = rememberDeviceType()
    var selectedSongForEdit by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Songs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(XBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val horizontalPadding = when (deviceType) {
            DeviceType.PHONE -> 16.dp
            DeviceType.TABLET -> 24.dp
            DeviceType.LARGE_TABLET -> 32.dp
        }
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "We need access to your music files",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = requestPermission) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: ${state.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No songs found. Add music to your device.")
                        }
                    } else {
                        when (deviceType) {
                            DeviceType.PHONE -> {
                                PhoneSongsLayout(
                                    songs = state.data,
                                    paddingValues = paddingValues,
                                    horizontalPadding = horizontalPadding,
                                    onEditSong = { song -> selectedSongForEdit = song }
                                )
                            }
                            DeviceType.TABLET -> {
                                TabletSongsLayout(
                                    songs = state.data,
                                    paddingValues = paddingValues,
                                    horizontalPadding = horizontalPadding,
                                    onEditSong = { song -> selectedSongForEdit = song }
                                )
                            }
                            DeviceType.LARGE_TABLET -> {
                                LargeTabletSongsLayout(
                                    songs = state.data,
                                    paddingValues = paddingValues,
                                    horizontalPadding = horizontalPadding,
                                    onEditSong = { song -> selectedSongForEdit = song }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Edit metadata dialog
    selectedSongForEdit?.let { song ->
        EditSongMetadataDialog(
            song = song,
            onDismiss = { selectedSongForEdit = null },
            onSave = { updatedSong ->
                // TODO: Update song in list
                selectedSongForEdit = null
            }
        )
    }
}

@Composable
private fun PhoneSongsLayout(
    songs: List<Song>,
    paddingValues: PaddingValues,
    horizontalPadding: Dp,
    onEditSong: (Song) -> Unit = {}
) {
    val playerViewModel = rememberPlayerViewModel()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = songs.size,
            key = { index -> songs[index].id }
        ) { index ->
            val song = songs[index]
            SongItemComponent(
                song = song,
                onClick = {
                    playerViewModel.setQueue(songs, index)
                },
                onMoreClick = {
                    onEditSong(song)
                }
            )
        }
    }
}

@Composable
private fun TabletSongsLayout(
    songs: List<Song>,
    paddingValues: PaddingValues,
    horizontalPadding: Dp,
    onEditSong: (Song) -> Unit = {}
) {
    val playerViewModel = rememberPlayerViewModel()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            count = songs.size,
            key = { index -> songs[index].id }
        ) { index ->
            val song = songs[index]
            SongItemComponent(
                song = song,
                onClick = {
                    playerViewModel.setQueue(songs, index)
                },
                onMoreClick = {
                    onEditSong(song)
                },
                padding = 16.dp
            )
        }
    }
}

@Composable
private fun LargeTabletSongsLayout(
    songs: List<Song>,
    paddingValues: PaddingValues,
    horizontalPadding: Dp,
    onEditSong: (Song) -> Unit = {}
) {
    val playerViewModel = rememberPlayerViewModel()
    // For large tablets, use a two-column grid layout
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = songs.size,
            key = { index -> songs[index].id }
        ) { index ->
            val song = songs[index]
            SongItemComponent(
                song = song,
                onClick = {
                    playerViewModel.setQueue(songs, index)
                },
                onMoreClick = {
                    onEditSong(song)
                },
                padding = 20.dp
            )
        }
    }
}



