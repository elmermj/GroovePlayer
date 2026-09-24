package com.aethelsoft.grooveplayer.presentation.library.mostplayed

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
import com.aethelsoft.grooveplayer.domain.model.playCountLabel
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING

@Composable
fun MostPlayedScreen(
    onNavigateBack: () -> Unit,
    viewModel: MostPlayedViewModel = hiltViewModel(),
) {
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val playerViewModel = rememberPlayerViewModel()
    val songs = mostPlayed.map { it.song }

    GrooveScreen(
        title = "Most played",
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        if (mostPlayed.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GrooveMutedText("No plays yet")
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
                verticalArrangement = Arrangement.spacedBy(XS_PADDING),
            ) {
                itemsIndexed(
                    items = mostPlayed,
                    key = { _, track -> track.song.id },
                ) { index, track ->
                    SongItemComponent(
                        song = track.song,
                        metaText = playCountLabel(track.playCount),
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