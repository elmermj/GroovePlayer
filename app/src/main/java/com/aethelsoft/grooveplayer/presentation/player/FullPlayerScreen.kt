package com.aethelsoft.grooveplayer.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.presentation.player.layouts.LargeTabletPlayerLayout
import com.aethelsoft.grooveplayer.presentation.player.layouts.PhonePlayerLayout
import com.aethelsoft.grooveplayer.presentation.player.layouts.TabletPlayerLayout
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.*
import com.aethelsoft.grooveplayer.presentation.player.ui.genreColor
import com.aethelsoft.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.PlayerControls
import com.aethelsoft.grooveplayer.presentation.player.ui.VolumeSlider
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
                bg = bg,
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
                bg = bg,
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
                bg = bg,
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