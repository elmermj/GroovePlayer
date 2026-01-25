package com.aethelsoft.grooveplayer.presentation.player

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.player.layouts.LargeTabletPlayerLayout
import com.aethelsoft.grooveplayer.presentation.player.layouts.PhonePlayerLayout
import com.aethelsoft.grooveplayer.presentation.player.layouts.TabletPlayerLayout
import com.aethelsoft.grooveplayer.presentation.player.ui.genreColor
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.rememberDeviceType

@Composable
fun FullPlayerScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onClose: () -> Unit = {},
    onOpen: () -> Unit = {}
) {
    val song by playerViewModel.currentSong.collectAsState()
    val pos by playerViewModel.position.collectAsState()
    val dur by playerViewModel.duration.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val shuffle by playerViewModel.shuffle.collectAsState()
    val repeat by playerViewModel.repeat.collectAsState()
    val isFullScreenPlayerOpened by playerViewModel.isFullScreenPlayerOpened.collectAsState()
    val deviceType = rememberDeviceType()

    // Set the state when screen is displayed, but only if not already open
    LaunchedEffect(Unit) {
        if (!isFullScreenPlayerOpened) {
            onOpen()
        }
    }

    Log.d("FullPlayerScreen SongDetails", "Title : ${song?.title} | Artist : ${song?.artist} | Genre : ${song?.genre} | Album : ${song?.album}")
    val bg by animateColorAsState(genreColor(song?.genre), label = "")

    when (deviceType) {
        DeviceType.PHONE -> {
            PhonePlayerLayout(
                song = song,
                pos = pos,
                dur = dur,
                isPlaying = isPlaying,
                shuffle = shuffle,
                repeat = repeat,
                playerViewModel = playerViewModel,
                onClose = onClose
            )
        }
        DeviceType.TABLET -> {
            TabletPlayerLayout(
                song = song,
                pos = pos,
                dur = dur,
                isPlaying = isPlaying,
                shuffle = shuffle,
                repeat = repeat,
                playerViewModel = playerViewModel,
                onClose = onClose
            )
        }
        DeviceType.LARGE_TABLET -> {
            LargeTabletPlayerLayout(
                song = song,
                pos = pos,
                dur = dur,
                isPlaying = isPlaying,
                shuffle = shuffle,
                repeat = repeat,
                playerViewModel = playerViewModel,
                onClose = onClose
            )
        }
    }
}

fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}