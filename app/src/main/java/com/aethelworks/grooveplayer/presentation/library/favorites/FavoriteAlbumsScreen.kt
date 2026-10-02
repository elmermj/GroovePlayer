package com.aethelworks.grooveplayer.presentation.library.favorites

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelworks.grooveplayer.domain.model.likedTrackLabel
import com.aethelworks.grooveplayer.domain.model.makeAlbumId
import com.aethelworks.grooveplayer.presentation.common.GrooveMutedText
import com.aethelworks.grooveplayer.presentation.common.GrooveScreen
import com.aethelworks.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelworks.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelworks.grooveplayer.presentation.common.MediaArtwork
import com.aethelworks.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.XS_PADDING

@Composable
fun FavoriteAlbumsScreen(
    onNavigateBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    viewModel: FavoriteAlbumsViewModel = hiltViewModel()
) {
    val favoriteAlbums = viewModel.favoriteAlbums.collectAsState(
        initial = emptyList()
    ).value

    GrooveScreen(
        title = stringResource(R.string.home_favorite_albums),
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        if (favoriteAlbums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GrooveMutedText(stringResource(R.string.library_like_album_here))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = M_PADDING,
                    end = M_PADDING,
                    top = topBarContentInset() + M_PADDING,
                    bottom = M_PADDING + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
                ),
                verticalArrangement = Arrangement.spacedBy(XS_PADDING),
            ) {
                items(
                    items = favoriteAlbums,
                    key = { album -> "${album.album}_${album.artist}" }
                ) { album ->
                    GrooveSurfaceCard(
                        onClick = {
                            onAlbumClick(makeAlbumId(album.artist, album.album))
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MediaArtwork(
                                url = album.artworkUrl,
                                kind = MediaArtworkKind.ALBUM,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                cornerRadius = S_PADDING,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.album,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                )
                                GrooveTinySpacer()
                                GrooveMutedText(
                                    text = "${album.artist} • ${likedTrackLabel(album.playCount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
