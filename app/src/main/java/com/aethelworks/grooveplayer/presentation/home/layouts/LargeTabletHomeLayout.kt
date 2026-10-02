package com.aethelworks.grooveplayer.presentation.home.layouts

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelworks.grooveplayer.presentation.common.UiState
import com.aethelworks.grooveplayer.presentation.home.HomeViewModel
import com.aethelworks.grooveplayer.presentation.home.ui.GenresLibraryCard
import com.aethelworks.grooveplayer.presentation.home.ui.LastPlayedSectionComponent
import com.aethelworks.grooveplayer.presentation.home.ui.LibraryCardComponent
import com.aethelworks.grooveplayer.presentation.home.ui.MostPlayedLibraryCard
import com.aethelworks.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.ui.TemplateVeritcalGridPage
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LargeTabletHomeLayout(
    viewModel: HomeViewModel,
    onNavigateToSongs: () -> Unit,
    onNavigateToGenres: () -> Unit,
    onNavigateToRecentlyPlayed: () -> Unit,
    onNavigateToMostPlayed: () -> Unit,
    onNavigateToFavoriteTracks: () -> Unit,
    onNavigateToFavoriteArtists: () -> Unit,
    onNavigateToFavoriteAlbums: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val favoriteArtists by viewModel.favoriteArtists.collectAsState()
    val favoriteAlbums by viewModel.favoriteAlbums.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val lastPlayedSongs by viewModel.lastPlayedSongs.collectAsState()

    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }

    /* ---------- INITIAL SPACER HEIGHT ---------- */
    val initialSpacerHeight =
        APP_BAR_HEIGHT +
                safeInsets.insets.asPaddingValues().calculateTopPadding() +
                12.dp
    val bottomSpacerHeight = safeInsets.insets.asPaddingValues().calculateBottomPadding()

    TemplateVeritcalGridPage(columns = 8) {
        if(lastPlayedSongs.isNotEmpty()){
            item(span = { GridItemSpan(maxLineSpan) }) {
                LastPlayedSectionComponent(
                    lastPlayedSongs = lastPlayedSongs,
                    currentSong = LocalPlayerViewModel.current?.currentSong?.collectAsState()?.value,
                    allLibrarySongs = viewModel.songs.ifEmpty { emptyList() }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }){
                Box(
                    modifier = Modifier
                        .padding(top = S_PADDING, bottom = S_PADDING)
                ){
                    Text(
                        text = stringResource(R.string.home_discover_more),
                        style = GrooveTheme.typography.sectionTitle.toTextStyle(),
                        color = GrooveTheme.colors.onSurface,
                    )
                }
            }
        }


        item(span = { GridItemSpan(2) }) {
            LibraryCardComponent(
                title = stringResource(R.string.home_all_songs),
                subtitle = if (viewModel.songs.isNotEmpty()) stringResource(R.string.count_songs, viewModel.songs.size) else stringResource(R.string.home_tap_to_browse),
                artworks = viewModel.songs.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = stringResource(R.string.home_no_songs),
                onClick = onNavigateToSongs
            )
        }
        item(span = { GridItemSpan(2) }) {
            LibraryCardComponent(
                title = stringResource(R.string.home_playlists),
                subtitle = if (playlists.isNotEmpty()) { if (playlists.size == 1) stringResource(R.string.count_playlists_one) else stringResource(R.string.count_playlists_other, playlists.size) } else stringResource(R.string.home_create_or_import),
                artworks = playlists.flatMap { it.artworkUrls },
                emptyNoticeText = stringResource(R.string.home_no_playlists),
                onClick = onNavigateToPlaylists,
            )
        }
        item(span = { GridItemSpan(2) }) {
            GenresLibraryCard(
                genres = genres,
                onClick = onNavigateToGenres,
            )
        }
        item(span = { GridItemSpan(2) }) {
            LibraryCardComponent(
                title = stringResource(R.string.home_recently_played),
                subtitle = if (recentlyPlayed.isNotEmpty()) stringResource(R.string.count_tracks, recentlyPlayed.size) else stringResource(R.string.home_no_recent),
                artworks = recentlyPlayed.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = stringResource(R.string.home_no_recent),
                onClick = onNavigateToRecentlyPlayed
            )
        }
        item(span = { GridItemSpan(2) }) {
            MostPlayedLibraryCard(
                mostPlayed = mostPlayed,
                onClick = onNavigateToMostPlayed,
            )
        }
        item(span = { GridItemSpan(2) }) {
            LibraryCardComponent(
                title = stringResource(R.string.home_favorite_tracks),
                subtitle = if (favoriteTracks.isNotEmpty()) stringResource(R.string.count_tracks, favoriteTracks.size) else stringResource(R.string.home_no_favorites),
                artworks = favoriteTracks.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = stringResource(R.string.home_no_favorites),
                onClick = onNavigateToFavoriteTracks
            )
        }
        item(span = { GridItemSpan(2)}){
            LibraryCardComponent(
                title = stringResource(R.string.home_favorite_albums),
                subtitle = if (favoriteAlbums.isNotEmpty()) stringResource(R.string.count_albums, favoriteAlbums.size) else stringResource(R.string.home_no_favorites),
                artworks = favoriteAlbums.map { item ->
                    item.artworkUrl.let { url ->
                        if (url.isNullOrEmpty()) {
                            "Unknown"
                        } else {
                            url
                        }
                    }
                },
                emptyNoticeText = stringResource(R.string.home_no_favorites),
                onClick = onNavigateToFavoriteAlbums
            )
        }
        item(span = { GridItemSpan(2) }) {
            LibraryCardComponent(
                title = stringResource(R.string.home_favorite_artists),
                subtitle = if (favoriteArtists.isNotEmpty()) stringResource(R.string.count_artists, favoriteArtists.size) else stringResource(R.string.home_no_favorites),
                artworks = emptyList(),
                emptyNoticeText = stringResource(R.string.home_no_favorites),
                onClick = onNavigateToFavoriteArtists
            )
        }
    }
}