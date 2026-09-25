package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.FullPlayerSongLike
import com.aethelsoft.grooveplayer.presentation.common.SongAvailabilityBadge
import com.aethelsoft.grooveplayer.presentation.common.rememberSongAvailabilityMark
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun SongDetails(
    song: Song?,
    isMiniPlayer: Boolean = false,
) {
    val typography = GrooveTheme.typography
    val colors = GrooveTheme.colors
    val availability = song?.let { rememberSongAvailabilityMark(it) }
    if (isMiniPlayer) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = song?.title ?: "",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = typography.miniPlayerSongTitle.toTextStyle(),
                    color = colors.onSurface,
                )
                if (availability != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    SongAvailabilityBadge(mark = availability, iconSize = 14.dp)
                }
            }
            Text(
                text = song?.artist ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = typography.miniPlayerSongArtist.toTextStyle(),
                color = colors.muted,
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = song?.title ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = typography.playerSongTitle.toTextStyle(),
                color = colors.onSurface,
            )
            Text(
                text = song?.artist ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = typography.playerSongArtist.toTextStyle(),
                color = colors.muted,
            )
            if (song != null) {
                Spacer(modifier = Modifier.height(4.dp))
                FullPlayerSongLike(song)
            }
            if (availability != null) {
                Spacer(modifier = Modifier.height(6.dp))
                SongAvailabilityBadge(mark = availability, iconSize = 18.dp)
            }
        }
    }
}
