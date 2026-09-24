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
    onNavigateToGenres: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToMostPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    BasePageTemplate(
        phoneLayout = {
            PhoneHomeLayout(
                viewModel = viewModel,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToGenres = onNavigateToGenres,
                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                onNavigateToMostPlayed = onNavigateToMostPlayed,
                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums,
            )
        },
        tabletLayout = {
            TabletHomeLayout(
                viewModel = viewModel,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToGenres = onNavigateToGenres,
                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                onNavigateToMostPlayed = onNavigateToMostPlayed,
                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
            )
        },
        largeTabletLayout = {
            LargeTabletHomeLayout(
                viewModel = viewModel,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToGenres = onNavigateToGenres,
                onNavigateToRecentlyPlayed = onNavigateToRecentlyPlayed,
                onNavigateToMostPlayed = onNavigateToMostPlayed,
                onNavigateToFavoriteTracks = onNavigateToFavoriteTracks,
                onNavigateToFavoriteArtists = onNavigateToFavoriteArtists,
                onNavigateToFavoriteAlbums = onNavigateToFavoriteAlbums
            )
        },
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        viewModel = viewModel,
        isSearchEnabled = true,
        pageTitle = "Your library",
        useSearchBar = true,
    )
}