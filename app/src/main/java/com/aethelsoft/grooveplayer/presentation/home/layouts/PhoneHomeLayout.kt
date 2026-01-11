package com.aethelsoft.grooveplayer.presentation.home.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.presentation.home.HomeViewModel
import com.aethelsoft.grooveplayer.presentation.home.ui.LibraryCardComponent

@Composable
public fun PhoneHomeLayout(
    state: UiState.Success<List<Song>>,
    viewModel: HomeViewModel,
    onNavigateToSongs: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit,
) {
    LazyVerticalGrid(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(top = 12.dp)
    ) {
        item {
            LibraryCardComponent(
                title = "All Songs",
                subtitle = if (state.data.isNotEmpty()) "${state.data.size} songs" else "Tap to browse",
                artworks = state.data.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = "No songs found",
                onClick = onNavigateToSongs
            )
        }
        item {
            LibraryCardComponent(
                title = "Recently Played",
                subtitle = "Your recent tracks",
                artworks = state.data.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = "No songs found",
                onClick = onNavigateToSongs
            )
        }
        item {
            LibraryCardComponent(
                title = "Favorites",
                subtitle = "Your favorite songs",
                artworks = state.data.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = "No songs found",
                onClick = onNavigateToSongs
            )
        }
    }
}