package com.aethelsoft.grooveplayer.presentation.library.genres

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun GenreTracksScreen(
    onNavigateBack: () -> Unit,
    viewModel: GenreTracksViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playerViewModel = rememberPlayerViewModel()
    val title = viewModel.genreName.ifBlank { "Genre" }

    GrooveScreen(
        title = title,
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        when {
            isLoading && songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GrooveTheme.colors.onSurface)
                }
            }
            songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    GrooveMutedText("No tracks in this genre")
                }
            }
            else -> {
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
                        items = songs,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        SongItemComponent(
                            song = song,
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
}
