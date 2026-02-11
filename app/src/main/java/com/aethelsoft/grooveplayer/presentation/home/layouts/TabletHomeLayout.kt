package com.aethelsoft.grooveplayer.presentation.home.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.presentation.home.HomeViewModel
import com.aethelsoft.grooveplayer.presentation.home.ui.LastPlayedSectionComponent
import com.aethelsoft.grooveplayer.presentation.home.ui.LibraryCardComponent
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.TemplateVeritcalGridPage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabletHomeLayout(
    viewModel: HomeViewModel,
    onNavigateToSongs: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val favoriteArtists by viewModel.favoriteArtists.collectAsState()
    val favoriteAlbums by viewModel.favoriteAlbums.collectAsState()
    val lastPlayedSongs by viewModel.lastPlayedSongs.collectAsState()

    TemplateVeritcalGridPage(columns = 3) {
        if(lastPlayedSongs.isNotEmpty()){
            item(span = { GridItemSpan(maxLineSpan) }) {
                LastPlayedSectionComponent(
                    lastPlayedSongs = lastPlayedSongs,
                    currentSong = LocalPlayerViewModel.current?.currentSong?.collectAsState()?.value,
                    allLibrarySongs = viewModel.songs
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }){
                Box(
                    modifier = Modifier
                        .padding(top = S_PADDING, bottom = S_PADDING)
                ){
                    Text(
                        text = "Discover More",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            LibraryCardComponent(
                title = "All Songs",
                subtitle = if (viewModel.songs.isNotEmpty()) "${viewModel.songs.size} songs" else "Tap to browse",
                artworks = viewModel.songs.map { item ->
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
                subtitle = if (recentlyPlayed.isNotEmpty()) "${recentlyPlayed.size} tracks" else "No recent tracks",
                artworks = recentlyPlayed.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = "No recent tracks",
                onClick = onNavigateToRecentlyPlayed
            )
        }
        item {
            LibraryCardComponent(
                title = "Favorite Tracks",
                subtitle = if (favoriteTracks.isNotEmpty()) "${favoriteTracks.size} tracks" else "No favorites yet",
                artworks = favoriteTracks.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = "No favorites yet",
                onClick = onNavigateToFavoriteTracks
            )
        }
        item {
            LibraryCardComponent(
                title = "Favorite Artists",
                subtitle = if (favoriteArtists.isNotEmpty()) "${favoriteArtists.size} artists" else "No favorites yet",
                artworks = emptyList(),
                emptyNoticeText = "No favorites yet",
                onClick = onNavigateToFavoriteArtists
            )
        }
        item {
            LibraryCardComponent(
                title = "Favorite Albums",
                subtitle = if (favoriteAlbums.isNotEmpty()) "${favoriteAlbums.size} albums" else "No favorites yet",
                artworks = emptyList(),
                emptyNoticeText = "No favorites yet",
                onClick = onNavigateToFavoriteAlbums
            )
        }
    }
}