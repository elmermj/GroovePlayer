package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.presentation.library.songs.EditSongMetadataDialog
import com.aethelsoft.grooveplayer.presentation.player.formatMillis
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.L_PADDING
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XAlbum
import com.aethelsoft.grooveplayer.utils.theme.icons.XAudioLines
import com.aethelsoft.grooveplayer.utils.theme.icons.XEdit
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun SongDetailsView(
    song: Song?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(bottom = L_PADDING),
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val spacing = GrooveTheme.spacing
    val radii = GrooveTheme.radii
    var showEditDialog by remember { mutableStateOf(false) }

    if (song == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(spacing.l),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No song playing",
                style = typography.body.toTextStyle(),
                color = colors.muted,
            )
        }
        return
    }

    val genreTint by animateColorAsState(
        targetValue = genreColor(song.genre).copy(alpha = 0.55f),
        label = "SongDetailsGenreGlow",
    )
    val albumName = song.album?.name?.takeIf { it.isNotBlank() }
    val genreText = genreLabel(song)
    val yearText = song.year?.takeIf { it > 0 }?.toString()
    val durationText = formatMillis(song.durationMs)
    val pathText = song.uri.takeIf { it.isNotBlank() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") {
            SongDetailsHero(
                song = song,
                genreTint = genreTint,
                onEditClick = { showEditDialog = true },
            )
        }

        item(key = "stats") {
            Spacer(modifier = Modifier.height(spacing.l))
            SongDetailsStatsRow(
                duration = durationText,
                year = yearText,
                genre = genreText.takeIf { it != "—" },
            )
        }

        item(key = "details") {
            Spacer(modifier = Modifier.height(spacing.l))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.m),
            ) {
                Text(
                    text = "Details",
                    style = typography.sectionTitle.toTextStyle(),
                    color = colors.onSurface.copy(alpha = 0.92f),
                    modifier = Modifier.padding(bottom = spacing.s),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(radii.cardShape)
                        .background(colors.surfaceRaised.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = colors.onSurface.copy(alpha = 0.06f),
                            shape = radii.cardShape,
                        )
                        .padding(horizontal = spacing.m, vertical = spacing.s),
                ) {
                    MetadataDetailRow(
                        icon = { Icon(XAlbum, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp)) },
                        label = "Album",
                        value = albumName ?: "Unknown album",
                    )
                    DetailDivider()
                    MetadataDetailRow(
                        icon = { Icon(XMusic, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp)) },
                        label = "Artist",
                        value = song.artist.ifBlank { "Unknown artist" },
                    )
                    DetailDivider()
                    MetadataDetailRow(
                        icon = { Icon(XAudioLines, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp)) },
                        label = "Genre",
                        value = genreText,
                    )
                    DetailDivider()
                    MetadataDetailRow(
                        icon = null,
                        label = "Year",
                        value = yearText ?: "—",
                    )
                    DetailDivider()
                    MetadataDetailRow(
                        icon = null,
                        label = "Duration",
                        value = durationText,
                    )
                }
            }
        }

        if (pathText != null) {
            item(key = "path") {
                Spacer(modifier = Modifier.height(spacing.l))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m),
                ) {
                    Text(
                        text = "File",
                        style = typography.sectionTitle.toTextStyle(),
                        color = colors.onSurface.copy(alpha = 0.92f),
                        modifier = Modifier.padding(bottom = spacing.s),
                    )
                    Text(
                        text = pathText,
                        style = typography.sectionItemSubtitle.toTextStyle(),
                        color = colors.muted.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.button))
                            .background(colors.surface.copy(alpha = 0.65f))
                            .padding(horizontal = spacing.m, vertical = spacing.s),
                    )
                }
            }
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(spacing.m))
        }
    }

    if (showEditDialog) {
        EditSongMetadataDialog(
            song = song,
            onDismiss = { showEditDialog = false },
            onSave = { showEditDialog = false },
        )
    }
}

@Composable
private fun SongDetailsHero(
    song: Song,
    genreTint: Color,
    onEditClick: () -> Unit,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val spacing = GrooveTheme.spacing
    // Match SwipeableArtwork's shape (20.dp corners) so the hero feels continuous with the player.
    val artworkRadius = 20.dp
    val artworkSize = playerArtworkSize()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val glowCenter = Offset(size.width / 2f, size.height * 0.38f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            genreTint.copy(alpha = 0.42f),
                            genreTint.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        center = glowCenter,
                        radius = size.minDimension * 0.72f,
                    ),
                    center = glowCenter,
                    radius = size.minDimension * 0.72f,
                )
            }
            .padding(horizontal = spacing.m)
            .padding(top = spacing.s),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .drawBehind {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.35f),
                            radius = size.minDimension * 0.48f,
                            center = Offset(size.width / 2f, size.height * 0.58f),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                MediaArtwork(
                    url = song.artworkUrl,
                    kind = MediaArtworkKind.SONG,
                    contentDescription = song.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colors.onSurface.copy(alpha = 0.18f),
                                    colors.onSurface.copy(alpha = 0.04f),
                                ),
                            ),
                            shape = RoundedCornerShape(artworkRadius),
                        ),
                    cornerRadius = artworkRadius,
                )
            }

            Spacer(modifier = Modifier.height(spacing.l))

            Text(
                text = song.title,
                style = typography.playerSongTitle.toTextStyle().copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 2f),
                        blurRadius = 8f,
                    ),
                ),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = song.artist.ifBlank { "Unknown artist" },
                style = typography.playerSongArtist.toTextStyle(),
                color = colors.muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing.m))

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, colors.onSurface.copy(alpha = 0.14f), CircleShape)
                    .clickable(onClick = onEditClick)
                    .padding(horizontal = spacing.m, vertical = spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = XEdit,
                    contentDescription = null,
                    tint = colors.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "Edit metadata",
                    style = typography.buttonLabel.toTextStyle(),
                    color = colors.onSurface.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun SongDetailsStatsRow(
    duration: String,
    year: String?,
    genre: String?,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val spacing = GrooveTheme.spacing
    val entries = buildList {
        add("Duration" to duration)
        if (year != null) add("Year" to year)
        if (genre != null) add("Genre" to genre)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.m),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(colors.onSurface.copy(alpha = 0.1f)),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = value,
                    style = typography.cardTitle.toTextStyle(),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = typography.sectionItemSubtitle.toTextStyle(),
                    color = colors.muted.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MetadataDetailRow(
    icon: (@Composable () -> Unit)?,
    label: String,
    value: String,
) {
    val colors = GrooveTheme.colors
    val typography = GrooveTheme.typography
    val spacing = GrooveTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(spacing.s))
        } else {
            Spacer(modifier = Modifier.width(28.dp + spacing.s))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = typography.sectionItemSubtitle.toTextStyle(),
                color = colors.muted.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = typography.body.toTextStyle(),
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Mirrors each player layout's SwipeableArtwork sizing:
 * - Phone: min(screenH * 0.6, screenW * 0.8) - S_PADDING
 * - Tablet / Large tablet: min(screenH * 0.4, screenW * 0.5) - S_PADDING
 */
@Composable
private fun playerArtworkSize(): Dp {
    val deviceType = rememberDeviceType()
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeight = containerSize.height.dp
    val screenWidth = containerSize.width.dp
    val maxArtworkHeight = when (deviceType) {
        DeviceType.PHONE -> minOf(screenHeight * 0.6f, screenWidth * 0.8f)
        DeviceType.TABLET,
        DeviceType.LARGE_TABLET -> minOf(screenHeight * 0.4f, screenWidth * 0.5f)
    }
    return maxArtworkHeight - S_PADDING
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = GrooveTheme.colors.onSurface.copy(alpha = 0.06f),
    )
}

private fun genreLabel(song: Song): String {
    val fromList = song.genres.map { it.name }.filter { it.isNotBlank() }
    return when {
        fromList.isNotEmpty() -> fromList.joinToString(", ")
        song.genre.isNotBlank() -> song.genre
        else -> "—"
    }
}
