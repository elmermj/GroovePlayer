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
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.S_PADDING
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
    val songsPagingItems: LazyPagingItems<Song> = viewModel.songsPagingFlow.collectAsLazyPagingItems()
    val deviceType = rememberDeviceType()
    var selectedSongForEdit by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Songs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(XBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    scrolledContainerColor = Color.Black,
                    actionIconContentColor = Color.White,
                    subtitleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
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
            when (deviceType) {
                DeviceType.PHONE -> {
                    PhoneSongsLayout(
                        songsPagingItems = songsPagingItems,
                        paddingValues = paddingValues,
                        horizontalPadding = horizontalPadding,
                        onEditSong = { song -> selectedSongForEdit = song }
                    )
                }
                DeviceType.TABLET -> {
                    TabletSongsLayout(
                        songsPagingItems = songsPagingItems,
                        paddingValues = paddingValues,
                        horizontalPadding = horizontalPadding,
                        onEditSong = { song -> selectedSongForEdit = song }
                    )
                }
                DeviceType.LARGE_TABLET -> {
                    LargeTabletSongsLayout(
                        songsPagingItems = songsPagingItems,
                        paddingValues = paddingValues,
                        horizontalPadding = horizontalPadding,
                        onEditSong = { song -> selectedSongForEdit = song }
                    )
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
    songsPagingItems: LazyPagingItems<Song>,
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
        verticalArrangement = Arrangement.spacedBy(S_PADDING / 2)
    ) {
        items(
            count = songsPagingItems.itemCount,
            key = songsPagingItems.itemKey { it.id }
        ) { index ->
            val song = songsPagingItems[index]
            if (song != null) {
                val list = songsPagingItems.itemSnapshotList.mapNotNull { it }
                SongItemComponent(
                    song = song,
                    onClick = {
                        if (index < list.size) {
                            playerViewModel.setQueue(list, index)
                        }
                    },
                    onMoreClick = { onEditSong(song) },
                    padding = 0.dp
                )
            }
        }
        songsPagingItems.loadState.apply {
            when {
                refresh is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                append is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                refresh is LoadState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error: ${(refresh as LoadState.Error).error.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(onClick = { songsPagingItems.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletSongsLayout(
    songsPagingItems: LazyPagingItems<Song>,
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
        verticalArrangement = Arrangement.spacedBy(S_PADDING / 2)
    ) {
        items(
            count = songsPagingItems.itemCount,
            key = songsPagingItems.itemKey { it.id }
        ) { index ->
            val song = songsPagingItems[index]
            if (song != null) {
                val list = songsPagingItems.itemSnapshotList.mapNotNull { it }
                SongItemComponent(
                    song = song,
                    onClick = {
                        if (index < list.size) {
                            playerViewModel.setQueue(list, index)
                        }
                    },
                    onMoreClick = { onEditSong(song) },
                    padding = 0.dp
                )
            }
        }
        songsPagingItems.loadState.apply {
            when {
                refresh is LoadState.Loading -> {
                    item {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                append is LoadState.Loading -> {
                    item {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                refresh is LoadState.Error -> {
                    item {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error: ${(refresh as LoadState.Error).error.message}", Modifier.padding(bottom = 16.dp))
                                Button(onClick = { songsPagingItems.retry() }) { Text("Retry") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeTabletSongsLayout(
    songsPagingItems: LazyPagingItems<Song>,
    paddingValues: PaddingValues,
    horizontalPadding: Dp,
    onEditSong: (Song) -> Unit = {}
) {
    val playerViewModel = rememberPlayerViewModel()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(S_PADDING / 2)
    ) {
        items(
            count = songsPagingItems.itemCount,
            key = songsPagingItems.itemKey { it.id }
        ) { index ->
            val song = songsPagingItems[index]
            if (song != null) {
                val list = songsPagingItems.itemSnapshotList.mapNotNull { it }
                SongItemComponent(
                    song = song,
                    onClick = {
                        if (index < list.size) {
                            playerViewModel.setQueue(list, index)
                        }
                    },
                    onMoreClick = { onEditSong(song) },
                    padding = 0.dp
                )
            }
        }
        songsPagingItems.loadState.apply {
            when {
                refresh is LoadState.Loading -> {
                    item {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                append is LoadState.Loading -> {
                    item {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                refresh is LoadState.Error -> {
                    item {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error: ${(refresh as LoadState.Error).error.message}", Modifier.padding(bottom = 16.dp))
                                Button(onClick = { songsPagingItems.retry() }) { Text("Retry") }
                            }
                        }
                    }
                }
            }
        }
    }
}



