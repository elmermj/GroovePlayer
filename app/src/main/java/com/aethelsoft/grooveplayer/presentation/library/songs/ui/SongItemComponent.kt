package com.aethelsoft.grooveplayer.presentation.library.songs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.DefaultSPadding
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun SongItemComponent(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    padding: Dp = DefaultSPadding
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(
                url = song.artworkUrl,
                kind = MediaArtworkKind.SONG,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                cornerRadius = S_PADDING,
            )
            Box(modifier = Modifier.width(S_PADDING))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = song.title,
                        style = GrooveTheme.typography.menuSongTitle.toTextStyle(),
                        color = GrooveTheme.colors.onSurface,
                    )
                }
                Text(
                    text = song.artist,
                    style = GrooveTheme.typography.menuSongArtist.toTextStyle(),
                    color = SoftWhite,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(song.durationMs),
                    style = GrooveTheme.typography.menuSongAlbum.toTextStyle(),
                    color = SoftWhite,
                )
                Box {
                    IconButton(
                        onClick = {
                            showOptionsMenu = true
                            onMoreClick()
                        }
                    ) {
                        Icon(
                            XMore,
                            contentDescription = "More options",
                            tint = SoftWhite,
                        )
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit song metadata") },
                            onClick = {
                                showOptionsMenu = false
                                onMoreClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}