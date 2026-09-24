package com.aethelsoft.grooveplayer.presentation.ui_customisation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionRow
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyle
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.LocalGrooveStyle


@Composable
fun LiveCustomisationPreview(style: GrooveStyle) {
    CompositionLocalProvider(LocalGrooveStyle provides style) {
        val colors = GrooveTheme.colors
        val type = GrooveTheme.typography
        val spacing = GrooveTheme.spacing
        val icons = GrooveTheme.iconSizes

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GrooveTheme.radii.cardShape)
                .background(colors.canvas)
                .border(1.dp, colors.muted.copy(alpha = 0.25f), GrooveTheme.radii.cardShape)
                .padding(spacing.m),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            Text(
                text = "Live preview",
                style = type.sectionItemSubtitle.toTextStyle(),
                color = colors.muted.copy(alpha = 0.7f),
            )
            Text(
                text = "Page title",
                style = type.pageTitle.toTextStyle(),
                color = colors.onSurface,
            )
            Text(
                text = "Section title",
                style = type.sectionTitle.toTextStyle(),
                color = colors.onSurface,
            )

            GrooveSurfaceCard {
                Text(
                    text = "Section item title",
                    style = type.sectionItemTitle.toTextStyle(),
                    color = colors.onSurface,
                )
                Text(
                    text = "Section item subtitle",
                    style = type.sectionItemSubtitle.toTextStyle(),
                    color = colors.muted,
                )
                Spacer(Modifier.height(spacing.s))
                Text(
                    text = "Card title",
                    style = type.cardTitle.toTextStyle(),
                    color = colors.onSurface,
                )
                Text(
                    text = "Card subtitle",
                    style = type.cardSubtitle.toTextStyle(),
                    color = colors.muted.copy(alpha = 0.7f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                MediaArtwork(
                    url = null,
                    kind = MediaArtworkKind.SONG,
                    modifier = Modifier.size(56.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Menu song title", style = type.menuSongTitle.toTextStyle(), color = colors.onSurface)
                    Text("Menu song artist", style = type.menuSongArtist.toTextStyle(), color = colors.muted)
                    Text("Menu song album", style = type.menuSongAlbum.toTextStyle(), color = colors.muted.copy(alpha = 0.75f))
                }
                Icon(
                    imageVector = XMusic,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(icons.md),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                MediaArtwork(
                    url = null,
                    kind = MediaArtworkKind.ALBUM,
                    modifier = Modifier.size(120.dp),
                )
                Spacer(Modifier.height(spacing.s))
                Text("Player song title", style = type.playerSongTitle.toTextStyle(), color = colors.onSurface)
                Text("Player song artist", style = type.playerSongArtist.toTextStyle(), color = colors.muted)
                Text("Player song album", style = type.playerSongAlbum.toTextStyle(), color = colors.muted.copy(alpha = 0.8f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GrooveTheme.radii.cardShape)
                    .background(colors.surface)
                    .padding(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                MediaArtwork(
                    url = null,
                    kind = MediaArtworkKind.SONG,
                    modifier = Modifier.size(40.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mini-player title", style = type.miniPlayerSongTitle.toTextStyle(), color = colors.onSurface)
                    Text("Mini-player artist", style = type.miniPlayerSongArtist.toTextStyle(), color = colors.muted)
                }
            }

            GrooveActionRow(
                primaryLabel = "Primary",
                onPrimary = {},
                secondaryLabel = "Secondary",
                onSecondary = {},
            )
        }
    }
}


@Composable
fun EditorSectionTitle(text: String) {
    Text(
        text = text,
        style = GrooveTheme.typography.sectionTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
        modifier = Modifier.padding(bottom = GrooveTheme.spacing.s),
    )
}

@Composable
fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = GrooveTheme.colors
    Text(
        text = label,
        style = GrooveTheme.typography.buttonLabel.toTextStyle(),
        color = if (selected) colors.onAccent else colors.onSurface,
        modifier = Modifier
            .clip(GrooveTheme.radii.chipShape)
            .background(if (selected) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = GrooveTheme.spacing.m, vertical = GrooveTheme.spacing.s),
    )
}

@Composable
fun ColorEditor(
    label: String,
    color: Color,
    onColor: (Color) -> Unit,
) {
    val colors = GrooveTheme.colors
    Column(modifier = Modifier.padding(bottom = GrooveTheme.spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, colors.muted.copy(alpha = 0.4f), CircleShape),
            )
            Spacer(Modifier.width(GrooveTheme.spacing.s))
            Text(label, style = GrooveTheme.typography.sectionItemTitle.toTextStyle(), color = colors.onSurface)
        }
        Spacer(Modifier.height(GrooveTheme.spacing.xs))
        Row(
            horizontalArrangement = Arrangement.spacedBy(GrooveTheme.spacing.xs),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            colorSwatches.forEach { swatch ->
                val selected = colorsMatch(swatch, color)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) colors.accent else colors.muted.copy(alpha = 0.3f),
                            shape = CircleShape,
                        )
                        .clickable { onColor(swatch) },
                )
            }
        }
    }
}


@Composable
fun DpSlider(
    label: String,
    value: Dp,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Dp) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = GrooveTheme.spacing.s)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = GrooveTheme.typography.sectionItemTitle.toTextStyle(), color = GrooveTheme.colors.onSurface)
            Text(
                "${"%.0f".format(value.value)} dp",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = GrooveTheme.colors.muted,
            )
        }
        Slider(
            value = value.value,
            onValueChange = { onChange(it.dp) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = GrooveTheme.colors.accent,
                activeTrackColor = GrooveTheme.colors.accent,
                inactiveTrackColor = GrooveTheme.colors.surface,
            ),
        )
    }
}

private fun colorsMatch(a: Color, b: Color): Boolean =
    a.toArgb() == b.toArgb()

private val colorSwatches = listOf(
    // Neutrals / canvases
    Color.Black,
    Color(0xFF070B14),
    Color(0xFF0C1210),
    Color(0xFF061214),
    Color(0xFF100E16),
    Color(0xFF120A08),
    Color(0xFF14100C),
    Color(0xFF0E0E0E),
    Color(0xFF121212),
    Color(0xFF1A1A1A),
    Color(0xFF1F1F1F),
    Color(0xFF212121),
    Color(0xFF2A2A2A),
    Color(0xFF443E3E),
    Color(0xFF5C5C5C),
    Color(0xFF888888),
    Color(0xFFC8C8C8),
    Color(0xFFDBDBDB),
    Color(0xFFE8E8E8),
    Color.White,
    // Greens / woodland
    Color(0xFF1A2420),
    Color(0xFF24302A),
    Color(0xFF354A3C),
    Color(0xFF4A6B52),
    Color(0xFF6B8F71),
    Color(0xFF828D6F),
    Color(0xFF8FBC8F),
    Color(0xFFA8BFA8),
    // Warm / desert / ember
    Color(0xFF2A2218),
    Color(0xFF3A3024),
    Color(0xFF5C4A35),
    Color(0xFFA67C52),
    Color(0xFFC4956A),
    Color(0xFFD4A574),
    Color(0xFFE8B86D),
    Color(0xFFFFB349),
    Color(0xFFE07050),
    Color(0xFFE07856),
    Color(0xFFFF4949),
    Color(0xFFCF6B6B),
    // Blues / ocean / eclipse
    Color(0xFF121A2A),
    Color(0xFF1A2438),
    Color(0xFF2A3A55),
    Color(0xFF3D5A80),
    Color(0xFF5B9FD4),
    Color(0xFF6B8FBF),
    Color(0xFFA0B4D4),
    Color(0xFF0E2428),
    Color(0xFF163438),
    Color(0xFF2A7A85),
    Color(0xFF3DB8C4),
    Color(0xFF5AA8B0),
    Color(0xFF98C8D0),
    // Lavender / violet
    Color(0xFF1E1A2A),
    Color(0xFF2A2438),
    Color(0xFF3E3555),
    Color(0xFF7C6BA8),
    Color(0xFFA78BFA),
    Color(0xFFB8A0E8),
    Color(0xFFC4B8D8),
    Color(0xFFE07090),
)