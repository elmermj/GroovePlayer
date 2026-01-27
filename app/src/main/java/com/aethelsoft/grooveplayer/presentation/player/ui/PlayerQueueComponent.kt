package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.animations.AudioWaveAnimation

@Composable
fun PlayerQueueComponent(
    currentSong: Song?,
    queue: List<Song>,
    onItemClick: (Song) -> Unit,
    maxHeight: Dp,
){
    val scrollableState = rememberScrollableState { delta -> delta }
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = M_PADDING)
            .heightIn(max = maxHeight)
            .width(360.dp)
            .scrollable(
                state = scrollableState,
                orientation = Orientation.Vertical,
                enabled = true,
                reverseDirection = false,
                flingBehavior = flingBehavior,
            )
    ) {
        items(count = queue.size) { index ->
            val song = queue[index]
            val isPlaying = currentSong?.id == song.id

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(360.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(song) }
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(S_PADDING))
                        Text(
                            text = song.title,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(XS_PADDING / 2))
                        Text(
                            text = song.artist,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(S_PADDING))
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = S_PADDING)
                    ) {
                        Spacer(modifier = Modifier.height(S_PADDING))
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.artworkUrl)
                                .size(36, 36)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(S_PADDING))
                    }
                }
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f), // left edge
                                    Color.Black.copy(alpha = 0.55f), // inner fade
                                    Color.Black.copy(alpha = 0.35f), // center
                                    Color.Black.copy(alpha = 0.55f), // inner fade
                                    Color.Black.copy(alpha = 0.85f)  // right edge
                                )
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioWaveAnimation(
                            waveHeight = 24.dp,
                            edgeFadeWidth = 36.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}