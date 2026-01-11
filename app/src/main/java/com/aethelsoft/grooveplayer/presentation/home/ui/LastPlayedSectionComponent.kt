package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.S_PADDING

@Composable
fun LastPlayedSectionComponent(
    lastPlayedSongs: List<Song>,
    currentSong: Song?,
    onNavigateToPlayer: () -> Unit
){
    val maxEightSongsList: List<Song> = lastPlayedSongs.take(8)
    val playerViewModel = LocalPlayerViewModel.current
    val isPlaying = playerViewModel?.isPlaying?.collectAsState()?.value

    LazyHorizontalGrid(
        rows = GridCells.Fixed(1),
        horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        verticalArrangement = Arrangement.spacedBy(S_PADDING),
    ) {
        items(maxEightSongsList.size){
            val song = maxEightSongsList[it]
            val singleArtworkUrl: List<String> = listOf(song.artworkUrl ?: "")
            LibraryCardComponent(
                title = song.title,
                subtitle = song.artist,
                artworks = singleArtworkUrl,
                onClick = {
                    // Scenario 1: No song is currently playing
                    if(currentSong == null){
                        playerViewModel?.setQueue(
                            songs = lastPlayedSongs,
                            startIndex = it
                        )
                        // It's good practice to have navigation handled outside the component
                        onNavigateToPlayer()
                    } else if(song.id == currentSong.id) {
                        // Scenario 2 & 3: The clicked song is the same as the current one
                        if (isPlaying == true) {
                            // Scenario 3: If playing, just open the player screen
                            onNavigateToPlayer()
                        } else {
                            // Scenario 2: If paused, resume playback
                            playerViewModel?.playPauseToggle()
                        }
                    } else {
                        // Bonus Scenario: A different song is playing
                        // Set the new queue and start playing the clicked song from the new list
                        playerViewModel?.setQueue(
                            songs = lastPlayedSongs,
                            startIndex = it
                        )
                    }
                },
                emptyNoticeText = "Artwork not available"
            )
        }
    }
}
