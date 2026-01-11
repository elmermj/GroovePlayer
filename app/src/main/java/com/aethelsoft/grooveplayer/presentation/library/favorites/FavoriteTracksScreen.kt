package com.aethelsoft.grooveplayer.presentation.library.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.songs.ui.SongItemComponent
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteTracksScreen(
    onNavigateBack: () -> Unit,
    viewModel: FavoriteTracksViewModel = hiltViewModel()
) {
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val playerViewModel = rememberPlayerViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Tracks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(XBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (favoriteTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No favorite tracks yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = favoriteTracks,
                    key = { _, song -> song.id }
                ) { index, song ->
                    SongItemComponent(
                        song = song,
                        onClick = {
                            playerViewModel.setQueue(favoriteTracks, index)
                        }
                    )
                }
            }
        }
    }
}

