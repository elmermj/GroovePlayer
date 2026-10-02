package com.aethelworks.grooveplayer.presentation.home.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.runtime.Composable
import com.aethelworks.grooveplayer.domain.model.LibraryGenre
import com.aethelworks.grooveplayer.domain.model.MostPlayedTrack

@Composable
internal fun GenresLibraryCard(
    genres: List<LibraryGenre>?,
    onClick: () -> Unit,
) {
    val subtitle = when {
        genres == null -> stringResource(R.string.home_tap_to_browse)
        genres.isEmpty() -> stringResource(R.string.home_no_genre_tags)
        genres.size == 1 -> stringResource(R.string.count_genre_one)
        else -> stringResource(R.string.count_genres, genres.size)
    }
    LibraryCardComponent(
        title = stringResource(R.string.home_genres),
        subtitle = subtitle,
        artworks = genres.orEmpty().map { genre -> genre.artworkUrl.toCardArtwork() },
        emptyNoticeText = stringResource(R.string.home_no_genre_tags),
        onClick = onClick,
    )
}

@Composable
internal fun MostPlayedLibraryCard(
    mostPlayed: List<MostPlayedTrack>,
    onClick: () -> Unit,
) {
    LibraryCardComponent(
        title = stringResource(R.string.home_most_played),
        subtitle = if (mostPlayed.isNotEmpty()) {
            stringResource(R.string.count_tracks, mostPlayed.size)
        } else {
            stringResource(R.string.home_no_plays)
        },
        artworks = mostPlayed.map { track -> track.song.artworkUrl.toCardArtwork() },
        emptyNoticeText = stringResource(R.string.home_no_plays),
        onClick = onClick,
    )
}

private fun String?.toCardArtwork(): String =
    if (isNullOrEmpty()) "Unknown" else this
