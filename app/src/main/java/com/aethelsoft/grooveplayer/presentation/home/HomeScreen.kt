package com.aethelsoft.grooveplayer.presentation.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aethelsoft.grooveplayer.presentation.home.layouts.LargeTabletHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.layouts.PhoneHomeLayout
import com.aethelsoft.grooveplayer.presentation.home.layouts.TabletHomeLayout
import com.aethelsoft.grooveplayer.presentation.common.BasePageTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSongs: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    BasePageTemplate(
        phoneLayout = {
            PhoneHomeLayout(
                viewModel = viewModel,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums,
            )
        },
        tabletLayout = {
            TabletHomeLayout(
                viewModel = viewModel,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
            )
        },
        largeTabletLayout = {
            LargeTabletHomeLayout(
                viewModel = viewModel,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
            )
        },
        onNavigateToSearch = onNavigateToSearch,
        viewModel = viewModel,
        isSearchEnabled = true,
        pageTitle = "Your library",
        useSearchBar = true,
    )
}