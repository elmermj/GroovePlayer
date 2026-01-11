package com.aethelsoft.grooveplayer.presentation.player.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.presentation.player.formatMillis
import com.aethelsoft.grooveplayer.presentation.player.ui.BluetoothBottomSheet
import com.aethelsoft.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.PlayerControls
import com.aethelsoft.grooveplayer.presentation.player.ui.VolumeSlider
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import com.aethelsoft.grooveplayer.utils.theme.icons.XBluetooth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonePlayerLayout(
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
    var showBluetoothSheet by remember { mutableStateOf(false) }

    if (showBluetoothSheet) {
        BluetoothBottomSheet(onDismiss = { showBluetoothSheet = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

        AsyncImage(
            model = song?.artworkUrl,
            contentDescription = "Artwork",
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(song?.title ?: "", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Text(song?.artist ?: "", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        CustomSlider(
            value = if (dur > 0) pos.toFloat() / dur else 0f,
            onValueChange = { frac ->
                val target = (frac * dur).toLong()
                playerViewModel.seekTo(target)
            },
            modifier = Modifier.fillMaxWidth(),
            height = 4.dp,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMillis(pos))
            Text(formatMillis(dur - pos))
        }

        Spacer(modifier = Modifier.height(24.dp))

        VolumeSlider(
            playerViewModel = playerViewModel,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = bg
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerControls(
            isMiniPlayer = false,
            isPlaying = isPlaying,
            shuffle = shuffle,
            repeat = repeat,
            playerViewModel = playerViewModel
        )

        Spacer(modifier = Modifier.height(12.dp))

        val queue by playerViewModel.queue.collectAsState()
        if (queue.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Up Next", style = MaterialTheme.typography.labelSmall)
                queue.take(5).forEach { q ->
                    Text(q.title, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}