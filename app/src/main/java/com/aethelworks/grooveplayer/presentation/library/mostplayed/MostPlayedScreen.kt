package com.aethelworks.grooveplayer.presentation.library.mostplayed

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.presentation.common.GrooveMutedText
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.SongListItemDefaults
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelworks.grooveplayer.utils.M_PADDING

@Composable
fun MostPlayedScreen(
    onNavigateBack: () -> Unit,
    viewModel: MostPlayedViewModel = hiltViewModel(),
) {
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val playerViewModel = rememberPlayerViewModel()
    val songs = mostPlayed.map { it.song }

    GrooveScreen(
        title = stringResource(R.string.home_most_played),
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        if (mostPlayed.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GrooveMutedText(stringResource(R.string.home_no_plays))
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
                    items = mostPlayed,
                    key = { _, track -> track.song.id },
                ) { index, track ->
                    SongItemComponent(
                        song = track.song,
                        playCount = if (track.playCount == 1) {
                            stringResource(R.string.count_plays_one)
                        } else {
                            stringResource(R.string.count_plays, track.playCount)
                        },
                        onClick = {
                            playerViewModel.setQueue(songs, index)
                        },
                        onPlayNext = { playerViewModel.playNext(it) },
                    )
                }
            }
        }
    }
}