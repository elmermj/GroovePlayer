package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.runtime.Composable
import com.aethelsoft.grooveplayer.domain.model.LibraryGenre
import com.aethelsoft.grooveplayer.domain.model.MostPlayedTrack

@Composable
internal fun GenresLibraryCard(
    genres: List<LibraryGenre>?,
    onClick: () -> Unit,
) {
    val subtitle = when {
        genres == null -> "Tap to browse"
        genres.isEmpty() -> "No genre tags"
        genres.size == 1 -> "1 genre"
        else -> "${genres.size} genres"
    }
    LibraryCardComponent(
        title = "Genres",
        subtitle = subtitle,
        artworks = genres.orEmpty().map { genre -> genre.artworkUrl.toCardArtwork() },
        emptyNoticeText = "No genre tags",
        onClick = onClick,
    )
}

@Composable
internal fun MostPlayedLibraryCard(
    mostPlayed: List<MostPlayedTrack>,
    onClick: () -> Unit,
) {
    LibraryCardComponent(
        title = "Most played",
        subtitle = if (mostPlayed.isNotEmpty()) {
            "${mostPlayed.size} tracks"
        } else {
            "No plays yet"
        },
        artworks = mostPlayed.map { track -> track.song.artworkUrl.toCardArtwork() },
        emptyNoticeText = "No plays yet",
        onClick = onClick,
    )
}

private fun String?.toCardArtwork(): String =
    if (isNullOrEmpty()) "Unknown" else this
