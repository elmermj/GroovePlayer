package com.aethelsoft.grooveplayer.presentation.library.ui

import XCheckCircle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Artist
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.SongAvailabilityBadge
import com.aethelsoft.grooveplayer.presentation.common.SongLikeButton
import com.aethelsoft.grooveplayer.presentation.common.rememberSongAvailabilityMark
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.presentation.common.rememberNavigationActions
import com.aethelsoft.grooveplayer.presentation.library.importing.LocalLibraryImport
import com.aethelsoft.grooveplayer.presentation.library.importing.rememberTrackPresence
import com.aethelsoft.grooveplayer.utils.DefaultSPadding
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XCircle
import com.aethelsoft.grooveplayer.utils.theme.icons.XEdit
import com.aethelsoft.grooveplayer.utils.theme.icons.XPlay
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore
import com.aethelsoft.grooveplayer.utils.theme.icons.XNFC
import com.aethelsoft.grooveplayer.utils.theme.icons.XWifiSync
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.HighlightPrimary
import com.aethelsoft.grooveplayer.utils.theme.ui.SingleLineMarqueeText
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

/**
 * Options configuration for GeneralItemComponent.
 * Pass non-null callbacks to show the corresponding option when expanded.
 */
data class ItemOptionsConfig(
    val onPlayNext: (() -> Unit)? = null,
    val onEditMetadata: (() -> Unit)? = null,
    val onShareViaTap: (() -> Unit)? = null,
    val onShareViaNearby: (() -> Unit)? = null,
)

/**
 * Selection configuration for GeneralItemComponent.
 * When provided, the item shows a checkbox and can be selected for multi-select flows.
 */
data class ItemSelectionConfig(
    val isSelected: Boolean,
    val onSelectedChange: (Boolean) -> Unit,
    val isSelectionMode: Boolean = true,
)

/**
 * Generic, reusable item component (artwork + title + subtitle + optional meta + optional menu)
 * that can be used for Song, Album, Artist, etc.
 *
 * When [optionsConfig] is provided, clicking the more icon expands options like ProfileSettingRow (EXPANDABLE).
 * Supports: Edit metadata (songs only), Share via tap, Share with nearby device.
 *
 * When [selectionConfig] is provided, shows a checkbox for multi-select.
 */
@Composable
fun GeneralItemComponent(
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    metaText: String? = null,
    onClick: () -> Unit,
    padding: Dp = DefaultSPadding,
    artworkKind: MediaArtworkKind = MediaArtworkKind.SONG,
    contentDescription: String? = null,
    optionsConfig: ItemOptionsConfig? = null,
    selectionConfig: ItemSelectionConfig? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    /** Decorative trailing content drawn before [metaText]. Null draws nothing and no spacer. */
    beforeMeta: (@Composable () -> Unit)? = null,
    contentAlpha: Float = 1f,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasOptions = optionsConfig != null && (
        optionsConfig.onPlayNext != null ||
        optionsConfig.onEditMetadata != null ||
        optionsConfig.onShareViaTap != null ||
        optionsConfig.onShareViaNearby != null
    )

    val effectiveOnClick = if (selectionConfig != null && selectionConfig.isSelectionMode) {
        { selectionConfig.onSelectedChange(!selectionConfig.isSelected) }
    } else {
        onClick
    }

    val inSelectionMode = selectionConfig != null && selectionConfig.isSelectionMode
    val isSelected = selectionConfig?.isSelected == true

    // Animated background highlight when selected
    val backgroundColor by animateColorAsState(
        targetValue = when {
            inSelectionMode && isSelected -> HighlightPrimary
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "ItemBackground"
    )

    // Tile-stepped-on effect: tilt + squash when select/unselect
    val stepAnimatable = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (inSelectionMode) {
            stepAnimatable.snapTo(0f)
            stepAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
            )
            stepAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
            )
        }
    }
    val stepRotationX = -stepAnimatable.value * 4f
    val stepScaleY = 1f - stepAnimatable.value * 0.2f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .clip(GrooveTheme.radii.cardShape)
            .background(backgroundColor)
            .graphicsLayer {
                rotationX = stepRotationX
                scaleY = stepScaleY
                transformOrigin = TransformOrigin.Center
            }
            .combinedClickable(
                onClick = effectiveOnClick,
                onLongClick = if (selectionConfig == null && onLongClick != null) onLongClick else null
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaArtwork(
                    url = artworkUrl,
                    kind = artworkKind,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(56.dp),
                    cornerRadius = S_PADDING,
                )
                Box(modifier = Modifier.width(S_PADDING))
                Column(modifier = Modifier.weight(1f)) {
                    SingleLineMarqueeText(
                        text = title,
                        style = GrooveTheme.typography.menuSongTitle.toTextStyle(),
                        color = GrooveTheme.colors.onSurface,
                    )
                    if (subtitle != null) {
                        SingleLineMarqueeText(
                            text = subtitle,
                            style = GrooveTheme.typography.menuSongArtist.toTextStyle(),
                            color = SoftWhite,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                beforeMeta?.invoke()
                if (metaText != null) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite,
                    )
                }
                when {
                    selectionConfig != null && selectionConfig.isSelectionMode -> {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = selectionConfig.isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "SelectionIcon"
                            ) { selected ->
                                Icon(
                                    imageVector = if (selected) XCheckCircle else XCircle,
                                    contentDescription = if (selected) "Selected" else "Unselected",
                                    tint = SoftWhite,
                                )
                            }
                        }
                    }
                    hasOptions -> {
                        IconButton(
                            onClick = { isExpanded = !isExpanded }
                        ) {
                            Icon(
                                XMore,
                                contentDescription = "More options",
                                tint = SoftWhite,
                            )
                        }
                    }
                }
            }
        }

        if (!inSelectionMode && (secondaryContent != null || hasOptions)) {
            AnimatedVisibility(
                visible = isExpanded && (secondaryContent != null || hasOptions),
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)),
                label = "GeneralItemExpandable"
            ) {
                if (secondaryContent != null) {
                    secondaryContent()
                } else if (optionsConfig != null) {
                    DefaultOptionsContent(
                        optionsConfig = optionsConfig,
                        horizontalPadding = padding,
                    )
                }
            }
        }
    }
}

private data class TrackStripAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun DefaultOptionsContent(
    optionsConfig: ItemOptionsConfig,
    horizontalPadding: Dp,
) {
    val actions = buildList {
        optionsConfig.onPlayNext?.let { onPlayNext ->
            add(TrackStripAction("Play next", XPlay, onPlayNext))
        }
        optionsConfig.onEditMetadata?.let { onEdit ->
            add(TrackStripAction("Edit song metadata", XEdit, onEdit))
        }
        optionsConfig.onShareViaTap?.let { onShare ->
            add(TrackStripAction("Tap to share", XNFC, onShare))
        }
        optionsConfig.onShareViaNearby?.let { onNearby ->
            add(TrackStripAction("Share with nearby", XWifiSync, onNearby))
        }
    }
    if (actions.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 2.dp,
                bottom = S_PADDING,
            ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        actions.forEach { action ->
            SlimTrackActionButton(
                label = action.label,
                icon = action.icon,
                onClick = action.onClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Equal-width icon-and-label control. No filled card: same quiet icon chrome as the player.
 * Long labels wrap onto a second line instead of truncating mid-word.
 */
@Composable
private fun SlimTrackActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GrooveTheme.colors
    val albumRole = GrooveTheme.typography.menuSongAlbum
    val labelSize = albumRole.fontSizeSp.coerceAtMost(12f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GrooveTheme.radii.button))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(GrooveTheme.iconSizes.sm),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = albumRole.toTextStyle().copy(
                fontSize = labelSize.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (labelSize * 1.15f).sp,
                textAlign = TextAlign.Center,
                hyphens = Hyphens.None,
            ),
            color = colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Song → GeneralItemComponent.
 */
@Composable
fun SongItemComponent(
    song: Song,
    onClick: () -> Unit,
    onEditMetadata: (Song) -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onLongPress: (Song) -> Unit = {},
    padding: Dp = DefaultSPadding,
    selectionConfig: ItemSelectionConfig? = null,
    metaText: String? = null,
    secondaryContent: (@Composable () -> Unit)? = null
) {
    val navigation = rememberNavigationActions()
    val availability = rememberSongAvailabilityMark(song)
    val presence = rememberTrackPresence(song)
    val import = LocalLibraryImport.current
    GeneralItemComponent(
        title = song.title,
        subtitle = if (presence.playable) song.artist else "${song.artist} · Unavailable",
        artworkUrl = song.artworkUrl,
        metaText = metaText ?: formatDuration(song.durationMs),
        onClick = { if (presence.playable) onClick() },
        contentAlpha = if (presence.playable) 1f else 0.45f,
        padding = padding,
        artworkKind = MediaArtworkKind.SONG,
        contentDescription = "${song.title} by ${song.artist}",
        optionsConfig = ItemOptionsConfig(
            onPlayNext = if (presence.playable) onPlayNext?.let { cb -> { cb(song) } } else null,
            onEditMetadata = { onEditMetadata(song) },
            onShareViaTap = if (presence.playable) {
                { navigation.openShareViaNfcWithSongs(listOf(song)) }
            } else {
                null
            },
            onShareViaNearby = if (presence.playable) {
                { navigation.openShareViaNearbyWithSongs(listOf(song)) }
            } else {
                null
            },
        ),
        selectionConfig = selectionConfig,
        onLongClick = { onLongPress(song) },
        secondaryContent = secondaryContent,
        beforeMeta = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SongLikeButton(song = song, iconSize = 20.dp)
                if (presence.canRestore) {
                    TextButton(onClick = { import.restoreSong(song) }) {
                        Text("Restore", color = HighlightPrimary)
                    }
                }
                if (availability != null) {
                    SongAvailabilityBadge(mark = availability, iconSize = 16.dp)
                }
            }
        },
    )
}

/**
 * Album → GeneralItemComponent.
 */
@Composable
fun AlbumItemComponent(
    album: Album,
    onClick: () -> Unit,
    padding: Dp = DefaultSPadding,
    optionsConfig: ItemOptionsConfig? = null,
    selectionConfig: ItemSelectionConfig? = null,
) {
    val navigation = rememberNavigationActions()
    val songsToShare = album.songs
    GeneralItemComponent(
        title = album.name,
        subtitle = album.artist,
        artworkUrl = album.artworkUrl,
        metaText = null,
        onClick = onClick,
        padding = padding,
        artworkKind = MediaArtworkKind.ALBUM,
        contentDescription = "${album.name} by ${album.artist}",
        optionsConfig = optionsConfig ?: if (songsToShare.isNotEmpty()) {
            ItemOptionsConfig(
                onShareViaTap = { navigation.openShareViaNfcWithSongs(songsToShare) },
                onShareViaNearby = { navigation.openShareViaNearbyWithSongs(songsToShare) }
            )
        } else null,
        selectionConfig = selectionConfig
    )
}

/**
 * Artist → GeneralItemComponent.
 */
@Composable
fun ArtistItemComponent(
    artist: Artist,
    onClick: () -> Unit,
    padding: Dp = DefaultSPadding,
    optionsConfig: ItemOptionsConfig? = null,
    selectionConfig: ItemSelectionConfig? = null,
) {
    GeneralItemComponent(
        title = artist.name,
        subtitle = null,
        artworkUrl = artist.imageUrl,
        metaText = null,
        onClick = onClick,
        padding = padding,
        artworkKind = MediaArtworkKind.ARTIST,
        contentDescription = artist.name,
        optionsConfig = optionsConfig,
        selectionConfig = selectionConfig
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
