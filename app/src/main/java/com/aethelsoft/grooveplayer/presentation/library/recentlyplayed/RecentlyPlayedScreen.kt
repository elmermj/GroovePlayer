package com.aethelsoft.grooveplayer.presentation.library.recentlyplayed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.songs.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyPlayedScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecentlyPlayedViewModel = hiltViewModel()
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playerViewModel = rememberPlayerViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Played") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(XBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (recentlyPlayed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No recently played tracks")
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
                    items = recentlyPlayed,
                    key = { _, song -> song.id }
                ) { index, song ->
                    SongItemComponent(
                        song = song,
                        onClick = {
                            playerViewModel.setQueue(recentlyPlayed, index)
                        }
                    )
                }
            }
        }
    }
}

