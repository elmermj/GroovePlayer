package com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.layouts

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.domain.model.PlaylistTrack
import com.aethelworks.grooveplayer.domain.model.PlaylistWithTracks
import com.aethelworks.grooveplayer.presentation.common.GrooveActionButton
import com.aethelworks.grooveplayer.presentation.common.GrooveMutedText
import com.aethelworks.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelworks.grooveplayer.presentation.library.playlists.PlaylistTrackList
import com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.PlaylistDetailViewModel
import com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.playPlaylist
import com.aethelworks.grooveplayer.presentation.library.playlists.playlist_detail.ui.RecommendationsList
import com.aethelworks.grooveplayer.presentation.player.PlayerViewModel
import com.aethelworks.grooveplayer.utils.XS_PADDING
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite
import kotlinx.coroutines.delay

@Composable
fun TabletPlaylistDetailLayout (
    playlist: PlaylistWithTracks?,
    tracks: List<PlaylistTrack>,
    isLoading: Boolean,
    message: String?,
    onAddTracks: () -> Unit,
    viewModel: PlaylistDetailViewModel,
    playerViewModel: PlayerViewModel
){

    val recommends by viewModel.recommends.collectAsState()
    val recommendationsShown by viewModel.isRecommendationShown.collectAsState()
    val pageReady = !isLoading && playlist != null
    var revealRecommendations by remember { mutableStateOf(false) }
    LaunchedEffect(pageReady) {
        if (!pageReady) {
            revealRecommendations = false
            viewModel.dismissSheet()
            return@LaunchedEffect
        }
        viewModel.showRecommendations()
        delay(1_000)
        revealRecommendations = true
    }

    when {
        isLoading && playlist == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GrooveTheme.colors.onSurface)
            }
        }
        playlist == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GrooveMutedText(stringResource(R.string.playlist_not_found))
            }
        }
        tracks.isEmpty() -> {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GrooveMutedText(stringResource(R.string.playlist_no_tracks))
                    GrooveTinySpacer()
                    TextButton(onClick = onAddTracks) {
                        Text(stringResource(R.string.playlist_add_tracks), color = SoftWhite)
                    }
                }
                AnimatedVisibility(
                    visible = revealRecommendations && recommendationsShown,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = 400),
                        initialOffsetX = { fullWidth -> fullWidth },
                    ) + fadeIn(animationSpec = tween(durationMillis = 400)),
                    exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ) + fadeOut(animationSpec = tween(durationMillis = 300)),
                ) {
                    RecommendationsList(
                        recommends = recommends,
                        onAddToPlaylist = viewModel::addToPlaylist,
                        onAddAll = viewModel::addAll,
                        onDismiss = viewModel::onSwipeToDismissRecommendationItem,
                        onDismissSheet = viewModel::dismissSheet,
                    )
                }
            }
        }
        else -> {
            PlaylistTrackList(
                tracks = tracks,
                onPlay = { index -> playPlaylist(playerViewModel, tracks, index) },
                onMove = viewModel::move,
                onRemove = viewModel::removeTrack,
                header = {
                    Column(verticalArrangement = Arrangement.spacedBy(XS_PADDING)) {
                        GrooveActionButton(
                            label = "Play",
                            onClick = {
                                playPlaylist(
                                    playerViewModel,
                                    tracks,
                                    tappedIndex = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!message.isNullOrBlank()) {
                            GrooveMutedText(
                                text = message.orEmpty(),
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                },
            )
        }
    }
}