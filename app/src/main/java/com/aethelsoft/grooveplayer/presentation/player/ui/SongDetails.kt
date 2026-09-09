package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun SongDetails(
    song: Song?,
    isMiniPlayer: Boolean = false,
) {
    val typography = GrooveTheme.typography
    val colors = GrooveTheme.colors
    Column(
        horizontalAlignment = if (isMiniPlayer) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        Text(
            text = song?.title ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = if (isMiniPlayer) {
                typography.miniPlayerSongTitle.toTextStyle()
            } else {
                typography.playerSongTitle.toTextStyle()
            },
            color = colors.onSurface,
        )

        Text(
            text = song?.artist ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = if (isMiniPlayer) {
                typography.miniPlayerSongArtist.toTextStyle()
            } else {
                typography.playerSongArtist.toTextStyle()
            },
            color = colors.muted,
        )
    }
}