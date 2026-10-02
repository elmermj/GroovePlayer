package com.aethelworks.grooveplayer.presentation.library.favorites

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
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
import com.aethelworks.grooveplayer.presentation.common.GrooveMutedText
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.presentation.common.SongListItemDefaults

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
        title = stringResource(R.string.home_favorite_tracks),
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        if (favoriteTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GrooveMutedText(stringResource(R.string.library_like_song_here))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = M_PADDING,
                    end = M_PADDING,
                    top = topBarContentInset() + M_PADDING,
                    bottom = M_PADDING + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
                ),
                verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
            ) {
                itemsIndexed(
                    items = favoriteTracks,
                    key = { _, song -> song.id }
                ) { index, song ->
                    SongItemComponent(
                        song = song,
                        onClick = {
                            playerViewModel.setQueue(favoriteTracks, index)
                        },
                        onPlayNext = { playerViewModel.playNext(it) },
                    )
                }
            }
        }
    }
}
