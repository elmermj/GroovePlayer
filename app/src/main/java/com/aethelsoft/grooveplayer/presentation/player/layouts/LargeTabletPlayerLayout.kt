package com.aethelsoft.grooveplayer.presentation.player.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.presentation.player.ui.AudioWaveformComponent
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.formatMillis
import com.aethelsoft.grooveplayer.presentation.player.ui.BluetoothBottomSheet
import com.aethelsoft.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.EqualizerControlsComponent
import com.aethelsoft.grooveplayer.presentation.player.ui.PlayerControls
import com.aethelsoft.grooveplayer.presentation.player.ui.QueueItemComponent
import com.aethelsoft.grooveplayer.presentation.player.ui.VolumeSlider
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeTabletPlayerLayout(
    song: com.aethelsoft.grooveplayer.domain.model.Song?,
    pos: Long,
    dur: Long,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: RepeatMode,
    playerViewModel: PlayerViewModel,
    bg: Color,
    onClose: () -> Unit
) {
    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showBluetoothSheet by remember { mutableStateOf(false) }
    val queue by playerViewModel.queue.collectAsState()

    if (showBluetoothSheet) {
        BluetoothBottomSheet(onDismiss = { showBluetoothSheet = false })
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Left side: Artwork
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(XBack, contentDescription = "Close")
                }
                IconButton(onClick = { showBluetoothSheet = true }) {
                    Icon(XBluetooth, contentDescription = "Bluetooth Devices")
                }
            }
            Spacer(
                modifier = Modifier.weight(1f)
            )
            AsyncImage(
                model = song?.artworkUrl,
                contentDescription = "Artwork",
                modifier = Modifier
                    .fillMaxHeight(0.4f)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                song?.title ?: "",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                song?.artist ?: "",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    CustomSlider(
                        value = if (dur > 0) pos.toFloat() / dur else 0f,
                        onValueChange = { frac ->
                            val target = (frac * dur).toLong()
                            playerViewModel.seekTo(target)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 8.dp,
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMillis(pos), style = MaterialTheme.typography.titleMedium)
                        Text(formatMillis(dur - pos), style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    VolumeSlider(
                        playerViewModel = playerViewModel,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = bg
                    )
                }

            }


            Spacer(modifier = Modifier.height(32.dp))

            PlayerControls(
                isMiniPlayer = false,
                isPlaying = isPlaying,
                shuffle = shuffle,
                repeat = repeat,
                playerViewModel = playerViewModel
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { showQueue = !showQueue }
                ) {
                    Text(
                        text = if (showQueue) "Hide Queue" else "Show Queue",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(XMore, contentDescription = "Queue", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equalizer control toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { showEqualizer = !showEqualizer }
                ) {
                    Text(
                        text = if (showEqualizer) "Hide Equalizer" else "Show Equalizer",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Equalizer controls (slides up from bottom)
            AnimatedVisibility(
                visible = showEqualizer,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                )
            ) {
                EqualizerControlsComponent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            }
        }

//        // Center side:
//        // 1. Audio graph
//        // 2. See queue/next track (will show Queue Info by pushing left and center side to make space for the right side)
//        // 3. Equalizer control (slide up from the bottom to push every widget in this section)
//        Column(
//            modifier = Modifier
//                .weight(if (showQueue) 0.8f else 1f)
//                .animateContentSize(animationSpec = tween(500, easing = FastOutSlowInEasing)),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            // Audio Graph/Waveform Visualization
//            Spacer(modifier = Modifier.weight(1f))
//            AudioWaveformComponent(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp),
//                isPlaying = isPlaying,
//                progress = if (dur > 0) pos.toFloat() / dur else 0f
//            )
//            Spacer(modifier = Modifier.weight(1f))
//
//            // Queue toggle button
//
//
//            Spacer(modifier = Modifier.weight(1f))
//        }

        // Right side: Queue info
        AnimatedVisibility(
            visible = showQueue,
            enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) + slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.weight(0.8f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (queue.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Queue",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        queue.take(10).forEachIndexed { index, q ->
                            QueueItemComponent(
                                song = q,
                                index = index,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        "Queue is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}