package com.aethelsoft.grooveplayer.presentation.player.ui

import android.icu.number.IntegerWidth
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING

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
        items(count = queue.size){ index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick =  {
                        onItemClick(queue[index])
                    })
                    .width(360.dp)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()

                        val brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Gray.copy(alpha = 0.2f),
                                Color.Gray.copy(alpha = 0.5f),
                                Color.White
                            ),
                            startX = 0f,
                            endX = size.width
                        )
                        drawLine(
                            brush = brush,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = S_PADDING)
                ) {
                    Spacer(modifier = Modifier.height(S_PADDING))
                    Text(
                        text = queue[index].title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(XS_PADDING/2))
                    Text(
                        text = queue[index].artist,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(S_PADDING))
                }
                AsyncImage(
                    model = queue[index].artworkUrl,
                    contentDescription = "Artwork",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .size(36.dp)
                        .clip(RoundedCornerShape(4f))
                )
            }
        }
    }
}