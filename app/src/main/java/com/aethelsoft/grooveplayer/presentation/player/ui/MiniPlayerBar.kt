package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.formatMillis
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth
import com.aethelsoft.grooveplayer.utils.theme.icons.XPause
import com.aethelsoft.grooveplayer.utils.theme.icons.XPlay
import com.aethelsoft.grooveplayer.utils.theme.ui.ToggledIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerBar(
    onMiniPlayerClicked: () -> Unit
) {
    val playerViewModel = rememberPlayerViewModel()
    val song by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val shuffle by playerViewModel.shuffle.collectAsState()
    val repeat by playerViewModel.repeat.collectAsState()
    val pos by playerViewModel.position.collectAsState()
    val dur by playerViewModel.duration.collectAsState()
    val deviceType = rememberDeviceType()
    var showBluetoothSheet by remember { mutableStateOf(false) }

    if (song == null) return

    val targetColor = genreColor(song?.genre)
    val bg by animateColorAsState(targetColor)
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (showBluetoothSheet) {
        BluetoothBottomSheet(onDismiss = { showBluetoothSheet = false })
    }

    when (deviceType) {
        DeviceType.PHONE -> {
            PhoneMiniPlayerBarContent(
                song = song!!,
                isPlaying = isPlaying,
                pos = pos,
                dur = dur,
                playerViewModel = playerViewModel,
                navigationBarPadding = navigationBarPadding,
                onMiniPlayerClicked = onMiniPlayerClicked
            )
        }
        DeviceType.TABLET, DeviceType.LARGE_TABLET -> {
            TabletMiniPlayerBarContent(
                song = song!!,
                isPlaying = isPlaying,
                shuffle = shuffle,
                repeat = repeat,
                pos = pos,
                dur = dur,
                bg = bg,
                playerViewModel = playerViewModel,
                navigationBarPadding = navigationBarPadding,
                showBluetoothIcon = deviceType == DeviceType.TABLET || deviceType == DeviceType.LARGE_TABLET,
                onShowBluetoothSheet = { showBluetoothSheet = true },
                onMiniPlayerClicked = onMiniPlayerClicked
            )
        }
    }
}

@Composable
private fun PhoneMiniPlayerBarContent(
    song: com.aethelsoft.grooveplayer.domain.model.Song,
    isPlaying: Boolean,
    pos: Long,
    dur: Long,
    playerViewModel: com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel,
    navigationBarPadding: androidx.compose.ui.unit.Dp,
    onMiniPlayerClicked: () -> Unit
) {
    // Swipe gesture state for phone layout
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val swipeThreshold = 100f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.95f),
                    )
                )
            )
            .padding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    }
                ) { change, dragAmount ->
                    dragOffsetX += dragAmount.x
                    dragOffsetY += dragAmount.y
                    
                    if (kotlin.math.abs(dragOffsetX) > kotlin.math.abs(dragOffsetY)) {
                        if (dragOffsetX > swipeThreshold) {
                            playerViewModel.previous()
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        } else if (dragOffsetX < -swipeThreshold) {
                            playerViewModel.next()
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        }
                    } else if (dragOffsetY > swipeThreshold) {
                        playerViewModel.stop()
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    }
                }
            }
            .clickable { onMiniPlayerClicked() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(S_PADDING))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(M_PADDING)
            ) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = "Artwork",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(180.dp)) {
                            SongDetails(song = song)
                        }
                        Spacer(modifier = Modifier.weight(1f).height(24.dp))
                        
                        // Play/Pause button only for phone
                        ToggledIconButton(
                            state = isPlaying,
                            onClick = { playerViewModel.playPauseToggle() }
                        ) { playing ->
                            if (playing) {
                                Icon(XPause, contentDescription = "Pause", tint = Color.White)
                            } else {
                                Icon(XPlay, contentDescription = "Play", tint = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f).height(24.dp))
                        Spacer(modifier = Modifier.width(56.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomSlider(
                            value = if (dur > 0) pos.toFloat() / dur else 0f,
                            onValueChange = { frac ->
                                val target = (frac * dur).toLong()
                                playerViewModel.seekTo(target)
                            },
                            modifier = Modifier.weight(1f),
                            height = 4.dp,
                            activeColor = Color.White,
                            inactiveColor = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            formatMillis(pos),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(navigationBarPadding))
        }
    }
}

@Composable
private fun TabletMiniPlayerBarContent(
    song: com.aethelsoft.grooveplayer.domain.model.Song,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: com.aethelsoft.grooveplayer.domain.model.RepeatMode,
    pos: Long,
    dur: Long,
    bg: Color,
    playerViewModel: com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel,
    navigationBarPadding: androidx.compose.ui.unit.Dp,
    showBluetoothIcon: Boolean,
    onShowBluetoothSheet: () -> Unit,
    onMiniPlayerClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.95f),
                    )
                )
            )
            .padding()
            .clickable { onMiniPlayerClicked() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(S_PADDING))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(M_PADDING)
            ) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = "Artwork",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(180.dp)) {
                            SongDetails(song = song)
                        }
                        Spacer(modifier = Modifier.weight(1f).height(24.dp))
                        
                        // All controls for tablet
                        PlayerControls(
                            isMiniPlayer = true,
                            isPlaying = isPlaying,
                            shuffle = shuffle,
                            repeat = repeat,
                            playerViewModel = playerViewModel
                        )
                        
                        Spacer(modifier = Modifier.weight(1f).height(24.dp))
                        
                        if (showBluetoothIcon) {
                            IconButton(onClick = onShowBluetoothSheet) {
                                Icon(XBluetooth, contentDescription = "Bluetooth Devices", tint = Color.White)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(56.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.width(180.dp)) {
                            VolumeSlider(
                                playerViewModel = playerViewModel,
                                backgroundColor = bg,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomSlider(
                            value = if (dur > 0) pos.toFloat() / dur else 0f,
                            onValueChange = { frac ->
                                val target = (frac * dur).toLong()
                                playerViewModel.seekTo(target)
                            },
                            modifier = Modifier.weight(1f),
                            height = 4.dp,
                            activeColor = Color.White,
                            inactiveColor = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            formatMillis(pos),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(navigationBarPadding))
        }
    }
}



