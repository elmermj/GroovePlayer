package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.LocalNavigation
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.utils.S_PADDING

@Composable
fun LastPlayedSectionComponent(
    lastPlayedSongs: List<Song>,
    allLibrarySongs: List<Song>,
    currentSong: Song?,
){
    // Use the latest list - this will update when the Flow emits
    val maxEightSongsList: List<Song> = remember(lastPlayedSongs) { 
        android.util.Log.d("LastPlayedSection", "List updated: ${lastPlayedSongs.joinToString { it.title }}")
        lastPlayedSongs.take(8)
    }
    val playerViewModel = LocalPlayerViewModel.current
    val isPlaying = playerViewModel?.isPlaying?.collectAsState()?.value
    val navigation = LocalNavigation.current
    
    // Get the latest queue for playback (not the captured parameter)
    val latestQueue = remember(lastPlayedSongs) { lastPlayedSongs }

    Column() {
        Box(
            modifier = Modifier.padding(bottom = S_PADDING)
        ){
            Text(
                text = "Where you left off",
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
            )
        }

        LazyHorizontalGrid(
            rows = GridCells.Fixed(1),
            modifier = Modifier.height(
                // screen width
                (LocalWindowInfo.current.containerSize.width.dp - (S_PADDING * 8 )) /8
            ),
            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            items(maxEightSongsList.size){ index ->
                val song = maxEightSongsList[index]
                val singleArtworkUrl: List<String> = listOf(song.artworkUrl ?: "")
                LibraryCardComponent(
                    title = song.title,
                    subtitle = song.artist,
                    artworks = singleArtworkUrl,
                    onClick = {
                        // Scenario 1: No song is currently playing
                        if(currentSong == null){
                            playerViewModel?.setQueueFromLastPlayedSongs(
                                songs = allLibrarySongs,  // Use all songs for endless queue
                                startSongId = song.id,
                            )
                        } else if(song.id == currentSong.id) {
                            // Scenario 2 & 3: The clicked song is the same as the current one
                            if (isPlaying == true) {
                                // Scenario 3: If playing, open the full player screen
                                navigation.openFullPlayer()
                            } else {
                                // Scenario 2: If paused, resume playback
                                playerViewModel?.playPauseToggle()
                            }
                        } else {
                            // Bonus Scenario: A different song is playing
                            playerViewModel?.setQueueFromLastPlayedSongs(
                                songs = allLibrarySongs,
                                startSongId = song.id,
                            )
                        }
                    },
                    emptyNoticeText = "Artwork not available"
                )
            }
        }
    }
}
