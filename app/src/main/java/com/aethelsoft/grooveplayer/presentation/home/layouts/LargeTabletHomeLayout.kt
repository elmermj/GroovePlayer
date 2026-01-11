package com.aethelsoft.grooveplayer.presentation.home.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.common.UiState
import com.aethelsoft.grooveplayer.presentation.home.HomeViewModel
import com.aethelsoft.grooveplayer.presentation.home.XAppBar
import com.aethelsoft.grooveplayer.presentation.home.ui.LastPlayedSectionComponent
import com.aethelsoft.grooveplayer.presentation.home.ui.LibraryCardComponent
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LargeTabletHomeLayout(
    state: UiState.Success<List<Song>>,
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

    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }

    /* ---------- INITIAL SPACER HEIGHT ---------- */
    val initialSpacerHeight =
        APP_BAR_HEIGHT +
                safeInsets.insets.asPaddingValues().calculateTopPadding() +
                12.dp
    val bottomSpacerHeight = safeInsets.insets.asPaddingValues().calculateBottomPadding()

    Column {

        LazyVerticalGrid(
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(M_PADDING),
            verticalArrangement = Arrangement.spacedBy(M_PADDING),
            columns = GridCells.Fixed(8),
            modifier = Modifier.padding(
                top = 20.dp,
                start = M_PADDING,
                end = M_PADDING
            )
        ) {
            if(lastPlayedSongs.isNotEmpty()){
                item(span = { GridItemSpan(maxLineSpan) }){
                    Box(
                        modifier = Modifier
                            .padding(top = initialSpacerHeight, bottom = S_PADDING)
                    ){
                        Text(
                            text = "Last Played Songs",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LastPlayedSectionComponent(
                        lastPlayedSongs = lastPlayedSongs,
                        onNavigateToSongs = onNavigateToSongs,
                        currentSong = LocalPlayerViewModel.current.currentSong
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
            item(span = { GridItemSpan(2) }) {
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
            item(span = { GridItemSpan(2) }) {
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
            item(span = { GridItemSpan(2) }) {
                LibraryCardComponent(
                    title = "Favorite Artists",
                    subtitle = if (favoriteArtists.isNotEmpty()) "${favoriteArtists.size} artists" else "No favorites yet",
                    artworks = emptyList(),
                    emptyNoticeText = "No favorites yet",
                    onClick = onNavigateToFavoriteArtists
                )
            }
            item(span = { GridItemSpan(2) }) {
                LibraryCardComponent(
                    title = "Favorite Albums",
                    subtitle = if (favoriteAlbums.isNotEmpty()) "${favoriteAlbums.size} albums" else "No favorites yet",
                    artworks = emptyList(),
                    emptyNoticeText = "No favorites yet",
                    onClick = onNavigateToFavoriteAlbums
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
            item {
                LibraryCardComponent(
                    title = "Favorite Albums",
                    subtitle = if (favoriteAlbums.isNotEmpty()) "${favoriteAlbums.size} albums" else "No favorites yet",
                    artworks = emptyList(),
                    emptyNoticeText = "No favorites yet",
                    onClick = onNavigateToFavoriteAlbums
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
            item {
                LibraryCardComponent(
                    title = "Favorite Albums",
                    subtitle = if (favoriteAlbums.isNotEmpty()) "${favoriteAlbums.size} albums" else "No favorites yet",
                    artworks = emptyList(),
                    emptyNoticeText = "No favorites yet",
                    onClick = onNavigateToFavoriteAlbums
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
            item {
                LibraryCardComponent(
                    title = "Favorite Albums",
                    subtitle = if (favoriteAlbums.isNotEmpty()) "${favoriteAlbums.size} albums" else "No favorites yet",
                    artworks = emptyList(),
                    emptyNoticeText = "No favorites yet",
                    onClick = onNavigateToFavoriteAlbums
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
            items(4){
                Spacer(modifier = Modifier.height(bottomSpacerHeight + 16.dp))
            }
        }
    }
}