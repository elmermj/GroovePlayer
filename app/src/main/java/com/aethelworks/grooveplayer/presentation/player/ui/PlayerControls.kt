package com.aethelworks.grooveplayer.presentation.player.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aethelworks.grooveplayer.domain.model.RepeatMode
import com.aethelworks.grooveplayer.presentation.player.PlayerViewModel
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.theme.icons.XPause
import com.aethelworks.grooveplayer.utils.theme.icons.XPlay
import com.aethelworks.grooveplayer.utils.theme.icons.XRepeatAll
import com.aethelworks.grooveplayer.utils.theme.icons.XRepeatOne
import com.aethelworks.grooveplayer.utils.theme.icons.XShuffle
import com.aethelworks.grooveplayer.utils.theme.icons.XSkipBack
import com.aethelworks.grooveplayer.utils.theme.icons.XSkipForward
import com.aethelworks.grooveplayer.utils.theme.ui.ToggledIconButton

@Composable
fun PlayerControls(
    isMiniPlayer: Boolean,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: RepeatMode,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    // Do not force fillMaxWidth here: LargeTablet/Tablet nest this in a Box with
    // side actions (volume / eq / queue). Stretching under those siblings caused
    // Visualization/transport collisions (BUG-001). Phone Column still centers wrap content.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {

        // Shuffle
        ToggledIconButton(
            state = shuffle,
            onClick = { playerViewModel.setShuffle(!shuffle) }
        ) { isShuffled ->
            Icon(
                XShuffle,
                contentDescription = stringResource(R.string.cd_shuffle),
                tint = if (isShuffled) Color.White else Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(M_PADDING))

        // Previous
        IconButton(onClick = { playerViewModel.previous() }) {
            Icon(XSkipBack, contentDescription = stringResource(R.string.cd_previous))
        }

        Spacer(modifier = Modifier.width(M_PADDING))

        // Play / Pause
        ToggledIconButton(
            state = isPlaying,
            onClick = { playerViewModel.playPauseToggle() }
        ) { playing ->
            if (playing) {
                Icon(XPause, contentDescription = stringResource(R.string.cd_pause))
            } else {
                Icon(XPlay, contentDescription = stringResource(R.string.cd_play))
            }
        }

        Spacer(modifier = Modifier.width(M_PADDING))

        // Next
        IconButton(onClick = { playerViewModel.next() }) {
            Icon(XSkipForward, contentDescription = stringResource(R.string.cd_next))
        }

        Spacer(modifier = Modifier.width(M_PADDING))

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
                    Icon(XRepeatAll, contentDescription = stringResource(R.string.cd_repeat_all))

                RepeatMode.OFF ->
                    Icon(
                        XRepeatAll,
                        contentDescription = stringResource(R.string.cd_repeat_off),
                        tint = Color.DarkGray
                    )

                RepeatMode.ONE ->
                    Icon(XRepeatOne, contentDescription = stringResource(R.string.cd_repeat_one))
            }
        }
    }
}

@Composable
fun BuildRepeatButtonIcon(repeatMode: RepeatMode){
    when (repeatMode){
        RepeatMode.ALL -> Icon(XRepeatAll, contentDescription = stringResource(R.string.cd_repeat_all))
        RepeatMode.OFF -> Icon(XRepeatAll, contentDescription = stringResource(R.string.cd_repeat_off), tint = Color.DarkGray)
        RepeatMode.ONE -> Icon(XRepeatOne, contentDescription = stringResource(R.string.cd_repeat_one))
    }
}