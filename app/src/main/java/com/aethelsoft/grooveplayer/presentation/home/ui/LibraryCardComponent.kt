package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkPlaceholder
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

/** Inset of the title/subtitle overlay. Continue-tile height reserves this plus the label lines. */
internal val LibraryCardOverlayPadding = 12.dp

@Composable
fun LibraryCardComponent(
    title: String,
    subtitle: String,
    artworks: List<String>,
    emptyNoticeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    square: Boolean = true,
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 1,
) {
    val uniqueArtworks = remember(artworks) {
        artworks
            .filter { it.isNotBlank() && it != "Unknown" }
            .distinct()
    }
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (square) Modifier.aspectRatio(1f) else Modifier)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            when (uniqueArtworks.size) {

                // No data
                0 -> {
                    MediaArtworkPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        kind = MediaArtworkKind.SONG,
                        cornerRadius = 0.dp,
                        contentDescription = emptyNoticeText,
                    )
                }

                // Only 1 data (no grid)
                1 -> {
                    ArtworkImage(
                        url = uniqueArtworks.first(),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // More than one, use grid logic
                else -> {
                    ArtworkGrid(
                        artworks = uniqueArtworks.take(4)
                    )
                }
            }

            // --- Gradient overlay ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.canvas.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // --- Title overlay ---
            // Wider continue tiles opt into extra lines; those labels fill the card so
            // ellipsis uses the tile width. Default cards keep the original wrap width.
            val widenLabels = titleMaxLines > 1 || subtitleMaxLines > 1
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(if (widenLabels) Modifier.fillMaxWidth() else Modifier)
                    .padding(LibraryCardOverlayPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    modifier = if (widenLabels) Modifier.fillMaxWidth() else Modifier,
                    style = typography.cardTitle.toTextStyle(),
                    color = colors.onSurface,
                    textAlign = if (widenLabels) TextAlign.Center else TextAlign.Unspecified,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    modifier = if (widenLabels) Modifier.fillMaxWidth() else Modifier,
                    style = typography.cardSubtitle.toTextStyle(),
                    color = colors.onSurface.copy(alpha = 0.85f),
                    textAlign = if (widenLabels) TextAlign.Center else TextAlign.Unspecified,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}