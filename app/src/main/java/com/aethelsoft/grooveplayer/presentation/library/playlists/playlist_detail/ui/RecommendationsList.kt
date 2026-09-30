package com.aethelsoft.grooveplayer.presentation.library.playlists.playlist_detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XClose
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsList(
    recommends: List<Song>,
    onAddToPlaylist: (song: Song) -> Unit,
    onAddAll: (songs: List<Song>) -> Unit,
    onDismiss: (song: Song) -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier
        .width(360.dp)
        .fillMaxHeight(),
) {
    Column(
        modifier = modifier
            .background(GrooveTheme.colors.canvas)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = M_PADDING, end = S_PADDING, top = S_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recommended",
                modifier = Modifier.weight(1f),
                style = GrooveTheme.typography.sectionTitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface,
            )
            TextButton(
                onClick = { onAddAll(recommends) },
                enabled = recommends.isNotEmpty(),
            ) {
                Text("Add all", color = SoftWhite)
            }
            IconButton(onClick = onDismissSheet) {
                Icon(
                    imageVector = XClose,
                    contentDescription = "Dismiss recommendations",
                    tint = GrooveTheme.colors.onSurface,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            recommends.forEach { song ->
                key(song.id) {
                    RecommendationRow(
                        song = song,
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onDismiss = { onDismiss(song) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationRow(
    song: Song,
    onAddToPlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    var removed by remember { mutableStateOf(false) }
    LaunchedEffect(dismissState.currentValue) {
        if (!removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            removed = true
            onDismiss()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val revealed = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            if (revealed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GrooveTheme.colors.error)
                        .padding(horizontal = M_PADDING),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = "Dismiss",
                        color = Color.White,
                        style = GrooveTheme.typography.menuSongTitle.toTextStyle(),
                    )
                }
            }
        },
    ) {
        Box(modifier = Modifier.background(GrooveTheme.colors.canvas)) {
            SongItemComponent(
                song = song,
                onClick = onAddToPlaylist,
            )
        }
    }
}
