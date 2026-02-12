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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Artist
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.rememberNavigationActions
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XAlbum
import com.aethelsoft.grooveplayer.utils.theme.icons.XArtist
import com.aethelsoft.grooveplayer.utils.theme.icons.XCircle
import com.aethelsoft.grooveplayer.utils.theme.icons.XEdit
import com.aethelsoft.grooveplayer.utils.theme.icons.XMore
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic
import com.aethelsoft.grooveplayer.utils.theme.icons.XNFC
import com.aethelsoft.grooveplayer.utils.theme.icons.XWifiSync
import com.aethelsoft.grooveplayer.utils.theme.ui.HighlightPrimary

/**
 * Options configuration for GeneralItemComponent.
 * Pass non-null callbacks to show the corresponding option when expanded.
 */
data class ItemOptionsConfig(
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
    padding: Dp = S_PADDING,
    defaultIcon: ImageVector,
    contentDescription: String? = null,
    optionsConfig: ItemOptionsConfig? = null,
    selectionConfig: ItemSelectionConfig? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasOptions = optionsConfig != null && (
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
            .clip(RoundedCornerShape(12.dp))
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
                if (artworkUrl == null) {
                    Box(modifier = Modifier.size(56.dp)) {
                        Icon(
                            defaultIcon,
                            contentDescription = contentDescription,
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White)
                        )
                    }
                } else {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(S_PADDING))
                    )
                }
                Box(modifier = Modifier.width(S_PADDING))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (metaText != null) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when {
                    selectionConfig != null && selectionConfig.isSelectionMode -> {
                        Box(
                            modifier = Modifier.size(40.dp),
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                } else if (hasOptions && optionsConfig != null) {
                    DefaultOptionsContent(optionsConfig)
                }
            }
        }
    }
}

@Composable
private fun DefaultOptionsContent(optionsConfig: ItemOptionsConfig) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer{
                clip = false
            }
            .padding(bottom = S_PADDING),
        horizontalArrangement = Arrangement.spacedBy(S_PADDING)
    ) {
        item {
            optionsConfig.onEditMetadata?.let { onEdit ->
                ExpandableOption(
                    label = "Edit song metadata",
                    icon = XEdit,
                    onClick = onEdit
                )
            }
        }
        item {
            optionsConfig.onShareViaTap?.let { onShare ->
                ExpandableOption(
                    label = "Tap to share",
                    icon = XNFC,
                    onClick = onShare
                )
            }
        }
        item {
            optionsConfig.onShareViaNearby?.let { onNearby ->
                ExpandableOption(
                    label = "Share with nearby device",
                    icon = XWifiSync,
                    onClick = onNearby
                )
            }
        }
    }
}

@Composable
private fun ExpandableOption(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1
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
    onLongPress: (Song) -> Unit = {},
    padding: Dp = S_PADDING,
    selectionConfig: ItemSelectionConfig? = null,
    secondaryContent: (@Composable () -> Unit)? = null
) {
    val navigation = rememberNavigationActions()
    GeneralItemComponent(
        title = song.title,
        subtitle = song.artist,
        artworkUrl = song.artworkUrl,
        metaText = formatDuration(song.durationMs),
        onClick = onClick,
        padding = padding,
        defaultIcon = XMusic,
        contentDescription = "${song.title} by ${song.artist}",
        optionsConfig = ItemOptionsConfig(
            onEditMetadata = { onEditMetadata(song) },
            onShareViaTap = { navigation.openShareViaNfcWithSongs(listOf(song)) },
            onShareViaNearby = { navigation.openShareViaNearbyWithSongs(listOf(song)) }
        ),
        selectionConfig = selectionConfig,
        onLongClick = { onLongPress(song) },
        secondaryContent = secondaryContent
    )
}

/**
 * Album → GeneralItemComponent.
 */
@Composable
fun AlbumItemComponent(
    album: Album,
    onClick: () -> Unit,
    padding: Dp = S_PADDING,
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
        defaultIcon = XAlbum,
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
    padding: Dp = S_PADDING,
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
        defaultIcon = XArtist,
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
