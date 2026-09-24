package com.aethelsoft.grooveplayer.presentation.common

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.library_category.ObserveLikedSongIdsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.library_category.ToggleSongLikeUseCase
import com.aethelsoft.grooveplayer.utils.theme.icons.XHeart
import com.aethelsoft.grooveplayer.utils.theme.icons.XHeartFilled
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import com.aethelsoft.grooveplayer.utils.theme.ui.ToggledIconButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongLikeViewModel @Inject constructor(
    observeLikedSongIdsUseCase: ObserveLikedSongIdsUseCase,
    private val toggleSongLikeUseCase: ToggleSongLikeUseCase,
) : ViewModel() {
    val likedSongIds: StateFlow<Set<String>> = observeLikedSongIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggle(song: Song) {
        viewModelScope.launch { toggleSongLikeUseCase(song) }
    }
}

@Composable
private fun rememberSongLikeViewModel(): SongLikeViewModel {
    val activity = LocalActivity.current as ComponentActivity
    return hiltViewModel(viewModelStoreOwner = activity)
}

/**
 * Heart for a song list row, the mini player, or the full player.
 * Likes are stored for every user; this control is not premium-gated.
 */
@Composable
fun SongLikeButton(
    song: Song,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    likedTint: Color = GrooveTheme.colors.accent,
    unlikedTint: Color = SoftWhite.copy(alpha = 0.72f),
) {
    val viewModel = rememberSongLikeViewModel()
    val likedIds by viewModel.likedSongIds.collectAsState()
    val liked = song.id in likedIds
    ToggledIconButton(
        state = liked,
        onClick = { viewModel.toggle(song) },
        modifier = modifier,
        size = 40.dp,
    ) { isLiked ->
        Icon(
            imageVector = if (isLiked) XHeartFilled else XHeart,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) likedTint else unlikedTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** White heart on the artwork gradient of the full player. */
@Composable
fun FullPlayerSongLike(song: Song?) {
    val current = song ?: return
    if (current.id.isBlank()) return
    Box(contentAlignment = Alignment.Center) {
        SongLikeButton(
            song = current,
            iconSize = 26.dp,
            likedTint = Color.White,
            unlikedTint = Color.White.copy(alpha = 0.55f),
        )
    }
}
