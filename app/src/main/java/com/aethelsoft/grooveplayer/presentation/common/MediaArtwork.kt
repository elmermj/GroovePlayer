package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.utils.theme.icons.XAlbum
import com.aethelsoft.grooveplayer.utils.theme.icons.XArtist
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

/** Media kind used to pick a matching placeholder glyph. */
enum class MediaArtworkKind {
    SONG,
    ALBUM,
    ARTIST,
}

fun MediaArtworkKind.placeholderIcon(): ImageVector = when (this) {
    MediaArtworkKind.SONG -> XMusic
    MediaArtworkKind.ALBUM -> XAlbum
    MediaArtworkKind.ARTIST -> XArtist
}

/**
 * Shared artwork surface for songs / albums / artists.
 * Missing or failed loads show [MediaArtworkPlaceholder] so empty art stays on-brand.
 *
 * @param cornerRadius null uses [GrooveTheme.radii.artwork] (user-configurable).
 */
@Composable
fun MediaArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    kind: MediaArtworkKind = MediaArtworkKind.SONG,
    contentDescription: String? = null,
    cornerRadius: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
    model: Any? = null,
    iconOverride: ImageVector? = null,
) {
    val radius = cornerRadius ?: GrooveTheme.radii.artwork
    val shape = RoundedCornerShape(radius)
    val resolvedModel = model ?: url
    var showPlaceholder by remember(resolvedModel) {
        mutableStateOf(resolvedModel == null || (resolvedModel is String && resolvedModel.isBlank()))
    }

    Box(modifier = modifier.clip(shape)) {
        if (!showPlaceholder) {
            AsyncImage(
                model = resolvedModel,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = { showPlaceholder = true },
            )
        }
        if (showPlaceholder) {
            MediaArtworkPlaceholder(
                modifier = Modifier.fillMaxSize(),
                kind = kind,
                cornerRadius = radius,
                contentDescription = contentDescription,
                iconOverride = iconOverride,
            )
        }
    }
}

/**
 * Dark chrome tile with a centered type icon — used when artwork is missing or fails to load.
 */
@Composable
fun MediaArtworkPlaceholder(
    modifier: Modifier = Modifier,
    kind: MediaArtworkKind = MediaArtworkKind.SONG,
    cornerRadius: Dp? = null,
    contentDescription: String? = null,
    iconOverride: ImageVector? = null,
) {
    val colors = GrooveTheme.colors
    val radius = cornerRadius ?: GrooveTheme.radii.artwork
    val shape = RoundedCornerShape(radius)
    val midTone = colors.onSurface.copy(alpha = 0.12f).compositeOver(colors.surface)

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(midTone, colors.surface, colors.canvas)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.onSurface.copy(alpha = 0.12f),
                        colors.onSurface.copy(alpha = 0.04f),
                    )
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val side = min(maxWidth, maxHeight)
        val iconSize = when {
            side < 40.dp -> side * 0.52f
            side < 96.dp -> side * 0.44f
            else -> side * 0.36f
        }
        val showHalo = side >= 48.dp

        if (showHalo) {
            Box(
                modifier = Modifier
                    .size(iconSize * 1.7f)
                    .border(
                        width = 1.5.dp,
                        color = colors.onSurface.copy(alpha = 0.08f),
                        shape = CircleShape,
                    )
            )
            Box(
                modifier = Modifier
                    .size(iconSize * 1.15f)
                    .background(
                        color = colors.onSurface.copy(alpha = 0.04f),
                        shape = CircleShape,
                    )
            )
        }

        Icon(
            imageVector = iconOverride ?: kind.placeholderIcon(),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = colors.muted.copy(alpha = 0.55f),
        )
    }
}
