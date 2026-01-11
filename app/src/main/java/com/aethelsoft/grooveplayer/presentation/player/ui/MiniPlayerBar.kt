package com.aethelsoft.grooveplayer.presentation.player.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.formatMillis
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth

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
    
    val showBluetoothIcon = deviceType == DeviceType.TABLET || deviceType == DeviceType.LARGE_TABLET

    if (showBluetoothSheet) {
        BluetoothBottomSheet(onDismiss = { showBluetoothSheet = false })
    }


//    MiniPlayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.5f * 1),
                        Color.Black.copy(alpha = 0.6f * 1),
                        Color.Black.copy(alpha = 0.95f * 1),
                    )
                )
            )
            .padding()
            .clickable { onMiniPlayerClicked() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(S_PADDING)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {

                AsyncImage(
                    model = song?.artworkUrl,
                    contentDescription = "Artwork",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                        ) {
                            SongDetails(
                                song = song
                            )
                        }
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        )
                        PlayerControls(
                            isMiniPlayer = true,
                            isPlaying = isPlaying,
                            shuffle = shuffle,
                            repeat = repeat,
                            playerViewModel = playerViewModel
                        )
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        )
                        if (showBluetoothIcon) {
                            IconButton(onClick = { showBluetoothSheet = true }) {
                                Icon(XBluetooth, contentDescription = "Bluetooth Devices", tint = Color.White)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(56.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                        ) {
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
                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )
                        Text(
                            formatMillis(pos),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    }
                }
            }
            // Extend the background beneath the navigation bar
            Spacer(modifier = Modifier.height(navigationBarPadding))
        }
    }
}

//@Composable
//fun MiniPlayerBackdrop(
//    height: Dp = 96.dp,
//    blurRadius: Dp = 20.dp,
//    darkAlpha: Float = 0.35f
//) {
//    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
//
//    Box(
//        modifier = Modifier
//            .align(Alignment.BottomCenter)
//            .fillMaxWidth()
//            .height(height)
//            .then(
//                if (supportsBlur) {
//                    Modifier.graphicsLayer {
//                        renderEffect = RenderEffect.createBlurEffect(
//                            blurRadius.toPx(),
//                            blurRadius.toPx(),
//                            Shader.TileMode.CLAMP
//                        )
//                    }
//                } else {
//                    Modifier
//                }
//            )
//            .background(
//                Brush.verticalGradient(
//                    colors = listOf(
//                        Color.Transparent,
//                        Color.Black.copy(alpha = darkAlpha * 0.6f),
//                        Color.Black.copy(alpha = darkAlpha)
//                    )
//                )
//            )
//    )
//}



