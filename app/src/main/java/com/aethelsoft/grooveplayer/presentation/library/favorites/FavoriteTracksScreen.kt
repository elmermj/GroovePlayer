package com.aethelsoft.grooveplayer.presentation.library.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING

@Composable
fun FavoriteTracksScreen(
    onNavigateBack: () -> Unit,
    viewModel: FavoriteTracksViewModel = hiltViewModel()
) {
    val favoriteTracks = viewModel.favoriteTracks.collectAsState(
        initial = emptyList()
    ).value

    val playerViewModel = rememberPlayerViewModel()
    GrooveScreen(
        title = "Favorite Tracks",
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        if (favoriteTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GrooveMutedText("No favorite tracks yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = M_PADDING, vertical = M_PADDING),
                verticalArrangement = Arrangement.spacedBy(XS_PADDING),
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
