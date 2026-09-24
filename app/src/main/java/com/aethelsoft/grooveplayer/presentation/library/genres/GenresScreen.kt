package com.aethelsoft.grooveplayer.presentation.library.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.LibraryGenre
import com.aethelsoft.grooveplayer.domain.model.trackCountLabel
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.GrooveTinySpacer
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.player.ui.genreColor
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun GenresScreen(
    onNavigateBack: () -> Unit,
    onGenreClick: (String) -> Unit,
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val genres by viewModel.genres.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    GrooveScreen(
        title = "Genres",
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        when {
            isLoading && genres.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GrooveTheme.colors.onSurface)
                }
            }
            genres.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    GrooveMutedText("No genre tags")
                }
            }
            else -> {
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
                        items = genres,
                        key = { genre -> genre.name.lowercase() },
                    ) { genre ->
                        GenreRow(
                            genre = genre,
                            onClick = { onGenreClick(genre.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreRow(
    genre: LibraryGenre,
    onClick: () -> Unit,
) {
    val tint = genreColor(genre.name)
    GrooveSurfaceCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S_PADDING),
        ) {
            if (genre.artworkUrl.isNullOrBlank()) {
                GenreSwatch(name = genre.name, tint = tint)
            } else {
                MediaArtwork(
                    url = genre.artworkUrl,
                    kind = MediaArtworkKind.ALBUM,
                    contentDescription = genre.name,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 12.dp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = genre.name,
                    style = GrooveTheme.typography.body.toTextStyle(),
                    fontWeight = FontWeight.Medium,
                    color = GrooveTheme.colors.onSurface,
                )
                GrooveTinySpacer()
                GrooveMutedText(text = trackCountLabel(genre.trackCount))
            }
        }
    }
}

@Composable
private fun GenreSwatch(
    name: String,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = 0.95f),
                        tint.copy(alpha = 0.45f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = GrooveTheme.typography.cardTitle.toTextStyle(),
            color = Color.White,
        )
    }
}
