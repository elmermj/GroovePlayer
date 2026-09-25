package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.LocalNavigation
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelsoft.grooveplayer.presentation.library.importing.rememberTrackPresence
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberAdaptiveWindowInfo
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.HighlightPrimary

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
    val windowInfo = rememberAdaptiveWindowInfo()
    
    // Get the latest queue for playback (not the captured parameter)
    val latestQueue = remember(lastPlayedSongs) { lastPlayedSongs }

    Column() {
        Box(
            modifier = Modifier.padding(bottom = S_PADDING)
        ){
            Text(
                text = "Where you left off",
                style = GrooveTheme.typography.sectionTitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Phone: four tiles plus the gaps fill the library content width.
            // Tablet and large tablet: pre-#16 square from the full window width,
            // (screen - 8 * spacing) / 8. DeviceType picks the formula.
            val contentWidthDp = if (constraints.hasBoundedWidth) {
                maxWidth.value
            } else {
                windowInfo.widthDp - (M_PADDING.value * 2f)
            }
            val typography = GrooveTheme.typography
            val density = LocalDensity.current
            val tile = continueTileSpec(
                deviceType = windowInfo.deviceType,
                contentWidthDp = contentWidthDp,
                screenWidthDp = windowInfo.widthDp,
                gapDp = S_PADDING.value,
                overlayPaddingDp = LibraryCardOverlayPadding.value,
                titleLineHeightDp = with(density) {
                    typography.cardTitle.toTextStyle().lineHeight.toDp().value
                },
                subtitleLineHeightDp = with(density) {
                    typography.cardSubtitle.toTextStyle().lineHeight.toDp().value
                },
            )
            val tileWidth = tile.widthDp.dp
            val tileHeight = tile.heightDp.dp

            LazyHorizontalGrid(
                rows = GridCells.Fixed(1),
                modifier = Modifier.height(tileHeight),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING),
            ) {
                items(maxEightSongsList.size) { index ->
                    val song = maxEightSongsList[index]
                    val presence = rememberTrackPresence(song)
                    val import = LocalLibraryImport.current
                    val singleArtworkUrl: List<String> = listOf(song.artworkUrl ?: "")
                    Box(
                        modifier = Modifier
                            .width(tileWidth)
                            .height(tileHeight)
                            .alpha(if (presence.playable) 1f else 0.45f),
                    ) {
                        LibraryCardComponent(
                            modifier = Modifier
                                .width(tileWidth)
                                .height(tileHeight),
                            square = tile.square,
                            titleMaxLines = tile.titleMaxLines,
                            subtitleMaxLines = tile.subtitleMaxLines,
                            title = song.title,
                            subtitle = if (presence.playable) song.artist else "${song.artist} · Unavailable",
                            artworks = singleArtworkUrl,
                            onClick = {
                                if (!presence.playable) return@LibraryCardComponent
                                if (currentSong == null) {
                                    playerViewModel?.setQueueFromLastPlayedSongs(
                                        songs = allLibrarySongs,
                                        startSongId = song.id,
                                    )
                                } else if (song.id == currentSong.id) {
                                    if (isPlaying == true) {
                                        navigation.openFullPlayer()
                                    } else {
                                        playerViewModel?.playPauseToggle()
                                    }
                                } else {
                                    playerViewModel?.setQueueFromLastPlayedSongs(
                                        songs = allLibrarySongs,
                                        startSongId = song.id,
                                    )
                                }
                            },
                            emptyNoticeText = "Artwork not available",
                        )
                        if (presence.canRestore) {
                            TextButton(
                                onClick = { import.restoreSong(song) },
                                modifier = Modifier.align(Alignment.TopCenter),
                            ) {
                                Text("Restore", color = HighlightPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
