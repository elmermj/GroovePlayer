package com.aethelworks.grooveplayer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.R
import com.aethelworks.grooveplayer.utils.theme.animations.AudioWaveAnimation
import com.aethelworks.grooveplayer.utils.theme.icons.XGripVertical
import com.aethelworks.grooveplayer.utils.theme.icons.XHeart
import com.aethelworks.grooveplayer.utils.theme.icons.XMore
import com.aethelworks.grooveplayer.utils.theme.ui.GroovePlayerTheme
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme

/**
 * Every song row and the playlist row use these sizes.
 * Colors stay on [GrooveTheme]; dimensions do not vary per screen.
 */
object SongListItemDefaults {
    val rowSpacing = 8.dp
    val cornerRadius = 16.dp
    val minHeight = 72.dp
    val horizontalPadding = 12.dp
    val verticalPadding = 12.dp
    val thumbnailSize = 48.dp
    val thumbnailCornerRadius = 10.dp
    val contentGap = 12.dp
    val heartSlot = 48.dp
    val durationSlot = 56.dp
    val playCountSlot = 72.dp
    val menuSlot = 48.dp
    val selectionSlot = 48.dp
    val dragSlot = 48.dp
    val slotTouch = 48.dp
    val iconSize = 22.dp
    val borderWidth = 1.dp

    /** Widest play-count label the play-count slot is sized for. */
    const val playCountReserve = "99 plays"
}

/**
 * Which fixed-width trailing slots a row shows.
 * A hidden slot is omitted; a shown slot always uses [SongListItemDefaults].
 * [reserveDrag] keeps the drag column without drawing a handle.
 */
@Immutable
data class SongListSlots(
    val favorite: Boolean = false,
    val duration: Boolean = false,
    val playCount: Boolean = false,
    val menu: Boolean = false,
    val selection: Boolean = false,
    val drag: Boolean = false,
    val reserveDrag: Boolean = false,
) {
    fun hasTrailing(): Boolean =
        favorite || duration || playCount || menu || selection || drag || reserveDrag

    /** Sum of the fixed trailing slots. Does not depend on the strings drawn inside them. */
    fun trailingWidthDp(): Float {
        var width = 0f
        if (favorite) width += SongListItemDefaults.heartSlot.value
        if (duration) width += SongListItemDefaults.durationSlot.value
        if (playCount) width += SongListItemDefaults.playCountSlot.value
        if (selection) width += SongListItemDefaults.selectionSlot.value
        else if (menu) width += SongListItemDefaults.menuSlot.value
        if (drag || reserveDrag) width += SongListItemDefaults.dragSlot.value
        return width
    }

    companion object {
        val Library = SongListSlots(favorite = true, duration = true, menu = true)
        val MostPlayed = SongListSlots(favorite = true, playCount = true, menu = true)
        val Playlist = SongListSlots(menu = true, drag = true)
        val NowPlaying = SongListSlots(reserveDrag = true)
        val UpNext = SongListSlots(drag = true)
        val None = SongListSlots()
    }
}

/**
 * Shared song row. One surface color, one corner radius, one thumbnail size.
 * The glass edge is the same stroke on every row; artwork is not sampled into the card.
 */
@Composable
fun SongListItem(
    title: String,
    artist: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    slots: SongListSlots = SongListSlots.Library,
    duration: String? = null,
    playCount: String? = null,
    highlighted: Boolean = false,
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    artworkContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    favorite: (@Composable () -> Unit)? = null,
    artworkBadge: (@Composable () -> Unit)? = null,
    extraTrailing: (@Composable () -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
    selection: (@Composable () -> Unit)? = null,
    dragHandleModifier: Modifier = Modifier,
    below: (@Composable () -> Unit)? = null,
) {
    val clickMod = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .songListCard(highlighted = highlighted, selected = selected),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SongListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(clickMod)
                    .padding(
                        start = SongListItemDefaults.horizontalPadding,
                        top = SongListItemDefaults.verticalPadding,
                        bottom = SongListItemDefaults.verticalPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SongThumbnail(
                    artworkUrl = artworkUrl,
                    artworkContentDescription = artworkContentDescription,
                    leading = leading,
                    highlighted = highlighted,
                    artworkBadge = artworkBadge,
                )
                Spacer(modifier = Modifier.width(SongListItemDefaults.contentGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = GrooveTheme.typography.menuSongTitle.toTextStyle(),
                        color = GrooveTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artist.isNotEmpty()) {
                        Text(
                            text = artist,
                            style = GrooveTheme.typography.menuSongArtist.toTextStyle(),
                            color = GrooveTheme.colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(
                    end = SongListItemDefaults.horizontalPadding,
                    top = SongListItemDefaults.verticalPadding,
                    bottom = SongListItemDefaults.verticalPadding,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (extraTrailing != null || slots.hasTrailing()) {
                    Spacer(modifier = Modifier.width(SongListItemDefaults.contentGap))
                }
                extraTrailing?.invoke()
                if (slots.favorite) {
                    FixedSlot(SongListItemDefaults.heartSlot) { favorite?.invoke() }
                }
                if (slots.duration) {
                    FixedSlot(SongListItemDefaults.durationSlot) {
                        if (duration != null) SlotLabel(duration)
                    }
                }
                if (slots.playCount) {
                    FixedSlot(SongListItemDefaults.playCountSlot) {
                        if (playCount != null) SlotLabel(playCount)
                    }
                }
                if (slots.selection) {
                    FixedSlot(SongListItemDefaults.selectionSlot) { selection?.invoke() }
                } else if (slots.menu) {
                    FixedSlot(SongListItemDefaults.menuSlot) { menu?.invoke() }
                }
                if (slots.drag) {
                    FixedSlot(SongListItemDefaults.dragSlot, modifier = dragHandleModifier) {
                        Icon(
                            imageVector = XGripVertical,
                            contentDescription = stringResource(R.string.cd_drag_to_reorder),
                            tint = GrooveTheme.colors.muted,
                            modifier = Modifier.size(SongListItemDefaults.iconSize),
                        )
                    }
                } else if (slots.reserveDrag) {
                    FixedSlot(SongListItemDefaults.dragSlot) {}
                }
            }
        }
        below?.invoke()
    }
}

/** Playlist rows share the song row container, radius, padding, and type styles. */
@Composable
fun PlaylistListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    SongListItem(
        title = title,
        artist = subtitle,
        artworkUrl = null,
        modifier = modifier,
        slots = SongListSlots(menu = true),
        onClick = onClick,
        leading = icon,
        menu = menu,
    )
}

/** Three-dot control that fills the menu slot. Dropdown content anchors to this box. */
@Composable
fun SongListOverflowIcon(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dropdown: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = XMore,
                contentDescription = contentDescription,
                tint = GrooveTheme.colors.muted,
                modifier = Modifier.size(SongListItemDefaults.iconSize),
            )
        }
        dropdown()
    }
}

@Composable
private fun SongThumbnail(
    artworkUrl: String?,
    artworkContentDescription: String?,
    leading: (@Composable () -> Unit)?,
    highlighted: Boolean,
    artworkBadge: (@Composable () -> Unit)?,
) {
    Box(
        modifier = Modifier
            .size(SongListItemDefaults.thumbnailSize)
            .clip(RoundedCornerShape(SongListItemDefaults.thumbnailCornerRadius)),
    ) {
        if (leading != null) {
            Box(modifier = Modifier.fillMaxSize()) { leading() }
        } else {
            MediaArtwork(
                url = artworkUrl,
                kind = MediaArtworkKind.SONG,
                contentDescription = artworkContentDescription,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = SongListItemDefaults.thumbnailCornerRadius,
                contentScale = ContentScale.Crop,
            )
        }
        if (highlighted) {
            val playing = stringResource(R.string.player_now_playing)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clearAndSetSemantics { contentDescription = playing },
                contentAlignment = Alignment.Center,
            ) {
                AudioWaveAnimation(
                    waveHeight = 18.dp,
                    modifier = Modifier.width(SongListItemDefaults.thumbnailSize - 8.dp),
                )
            }
        }
        if (artworkBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp),
            ) {
                artworkBadge()
            }
        }
    }
}

@Composable
private fun SlotLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        textAlign = TextAlign.End,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        style = GrooveTheme.typography.menuSongAlbum.toTextStyle(),
        color = GrooveTheme.colors.muted,
    )
}

@Composable
private fun FixedSlot(
    width: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(SongListItemDefaults.slotTouch)
            .then(modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Solid theme surface plus one glass edge. The edge does not sample album art,
 * so every row stays the same color.
 */
@Composable
private fun Modifier.songListCard(highlighted: Boolean, selected: Boolean): Modifier {
    val colors = GrooveTheme.colors
    val shape = RoundedCornerShape(SongListItemDefaults.cornerRadius)
    val fill = when {
        selected -> colors.accent.copy(alpha = 0.16f).compositeOver(colors.surface)
        highlighted -> colors.accent.copy(alpha = 0.08f).compositeOver(colors.surface)
        else -> colors.surface
    }
    val borderColor = when {
        highlighted -> colors.accent
        selected -> colors.accent.copy(alpha = 0.7f)
        else -> colors.onSurface.copy(alpha = 0.10f)
    }
    return this
        .border(SongListItemDefaults.borderWidth, borderColor, shape)
        .clip(shape)
        .background(fill)
        .drawWithContent {
            drawContent()
            val stroke = SongListItemDefaults.borderWidth.toPx()
            val radius = SongListItemDefaults.cornerRadius.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
                start = Offset(radius * 0.5f, stroke),
                end = Offset(size.width * 0.7f, stroke),
                strokeWidth = stroke,
            )
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Library and long title")
@Composable
private fun SongListItemLibraryPreview() {
    GroovePlayerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
        ) {
            PreviewSongRow(
                title = "Be Alright",
                artist = "Dean Lewis",
                duration = "3:19",
            )
            PreviewSongRow(
                title = "Heaven Takes You Home (feat. Connie Constance)",
                artist = "Swedish House Mafia/Connie Constance",
                duration = "4:17",
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "1 play and 2 plays")
@Composable
private fun SongListItemPlayCountPreview() {
    GroovePlayerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
        ) {
            PreviewPlayCountRow(
                title = "Can't Let Go",
                artist = "Caught A Ghost",
                count = stringResource(R.string.count_plays_one),
            )
            PreviewPlayCountRow(
                title = "Bliss",
                artist = "Muse",
                count = stringResource(R.string.count_plays, 2),
            )
            PreviewPlayCountRow(
                title = "Reserved width",
                artist = "Slot stays put",
                count = stringResource(R.string.count_plays, 99),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Playlist, now playing, up next")
@Composable
private fun SongListItemQueuePreview() {
    GroovePlayerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(SongListItemDefaults.rowSpacing),
        ) {
            SongListItem(
                title = "End of Beginning",
                artist = "Djo",
                artworkUrl = null,
                slots = SongListSlots.Playlist,
                menu = { SongListOverflowIcon(contentDescription = stringResource(R.string.cd_more_options), onClick = {}) },
            )
            SongListItem(
                title = "Dreamers",
                artist = "Savoir Adore",
                artworkUrl = null,
                slots = SongListSlots.NowPlaying,
                highlighted = true,
            )
            SongListItem(
                title = "greedy",
                artist = "Tate McRae",
                artworkUrl = null,
                slots = SongListSlots.UpNext,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, name = "Playlist list")
@Composable
private fun PlaylistListItemPreview() {
    GroovePlayerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlaylistListItem(
                title = "Bass Boosted",
                subtitle = "10 tracks",
                onClick = {},
                icon = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF26A69A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = XHeart,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                },
                menu = { SongListOverflowIcon(contentDescription = stringResource(R.string.cd_playlist_options), onClick = {}) },
            )
        }
    }
}

@Composable
private fun PreviewSongRow(title: String, artist: String, duration: String) {
    SongListItem(
        title = title,
        artist = artist,
        artworkUrl = null,
        slots = SongListSlots.Library,
        duration = duration,
        favorite = {
            Icon(
                imageVector = XHeart,
                contentDescription = null,
                tint = GrooveTheme.colors.muted,
                modifier = Modifier.size(SongListItemDefaults.iconSize),
            )
        },
        menu = { SongListOverflowIcon(contentDescription = stringResource(R.string.cd_more_options), onClick = {}) },
    )
}

@Composable
private fun PreviewPlayCountRow(title: String, artist: String, count: String) {
    SongListItem(
        title = title,
        artist = artist,
        artworkUrl = null,
        slots = SongListSlots.MostPlayed,
        playCount = count,
        favorite = {
            Icon(
                imageVector = XHeart,
                contentDescription = null,
                tint = GrooveTheme.colors.muted,
                modifier = Modifier.size(SongListItemDefaults.iconSize),
            )
        },
        menu = { SongListOverflowIcon(contentDescription = stringResource(R.string.cd_more_options), onClick = {}) },
    )
}
