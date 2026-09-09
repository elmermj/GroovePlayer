package com.aethelsoft.grooveplayer.presentation.library.favorites

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
import com.aethelsoft.grooveplayer.domain.model.makeAlbumId
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING

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
        title = "Favorite Albums",
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        if (favoriteAlbums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GrooveMutedText("No favorite albums yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = M_PADDING, vertical = M_PADDING),
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
                                    text = "${album.artist} • ${album.playCount} plays",
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
