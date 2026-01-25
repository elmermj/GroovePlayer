package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.presentation.player.PlayerViewModel
import com.aethelsoft.grooveplayer.utils.theme.icons.XPause
import com.aethelsoft.grooveplayer.utils.theme.icons.XPlay
import com.aethelsoft.grooveplayer.utils.theme.icons.XRepeatAll
import com.aethelsoft.grooveplayer.utils.theme.icons.XRepeatOne
import com.aethelsoft.grooveplayer.utils.theme.icons.XShuffle
import com.aethelsoft.grooveplayer.utils.theme.icons.XSkipBack
import com.aethelsoft.grooveplayer.utils.theme.icons.XSkipForward
import com.aethelsoft.grooveplayer.utils.theme.ui.ToggledIconButton

@Composable
fun PlayerControls(
    isMiniPlayer: Boolean,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: RepeatMode,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = if (!isMiniPlayer) modifier.fillMaxWidth() else modifier
    ) {

        // Shuffle
        ToggledIconButton(
            state = shuffle,
            onClick = { playerViewModel.setShuffle(!shuffle) }
        ) { isShuffled ->
            Icon(
                XShuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffled) Color.White else Color.White.copy(alpha = 0.6f)
            )
        }

        // Previous
        IconButton(onClick = { playerViewModel.previous() }) {
            Icon(XSkipBack, contentDescription = "Previous")
        }

        // Play / Pause
        ToggledIconButton(
            state = isPlaying,
            onClick = { playerViewModel.playPauseToggle() }
        ) { playing ->
            if (playing) {
                Icon(XPause, contentDescription = "Pause")
            } else {
                Icon(XPlay, contentDescription = "Play")
            }
        }

        // Next
        IconButton(onClick = { playerViewModel.next() }) {
            Icon(XSkipForward, contentDescription = "Next")
        }

        // Repeat (Enum)
        ToggledIconButton(
            state = repeat,
            onClick = {
                val next = when (repeat) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                playerViewModel.setRepeat(next)
            }
        ) { repeatMode ->
            when (repeatMode) {
                RepeatMode.ALL ->
                    Icon(XRepeatAll, contentDescription = "Repeat All")

                RepeatMode.OFF ->
                    Icon(
                        XRepeatAll,
                        contentDescription = "Repeat Off",
                        tint = Color.DarkGray
                    )

                RepeatMode.ONE ->
                    Icon(XRepeatOne, contentDescription = "Repeat One")
            }
        }
    }
}

@Composable
fun BuildRepeatButtonIcon(repeatMode: RepeatMode){
    when (repeatMode){
        RepeatMode.ALL -> Icon(XRepeatAll, contentDescription = "Repeat All")
        RepeatMode.OFF -> Icon(XRepeatAll, contentDescription = "Repeat Off", tint = Color.DarkGray)
        RepeatMode.ONE -> Icon(XRepeatOne, contentDescription = "Repeat One")
    }
}