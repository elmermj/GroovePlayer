package com.aethelsoft.grooveplayer.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionRow
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.presentation.common.MediaArtwork
import com.aethelsoft.grooveplayer.presentation.common.MediaArtworkKind
import com.aethelsoft.grooveplayer.utils.theme.icons.XMusic
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyle
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCodec
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleIds
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTypographyScale
import com.aethelsoft.grooveplayer.utils.theme.ui.LocalGrooveStyle

private enum class UiStylingPrompt {
    None,
    DiscardOnBack,
    ConfirmSave,
}

@Composable
fun UiStylingScreen(
    onNavigateBack: () -> Unit,
    viewModel: UiStylingViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val isReady by viewModel.isReady.collectAsState()
    var prompt by remember { mutableStateOf(UiStylingPrompt.None) }

    fun requestBack() {
        if (isDirty) {
            prompt = UiStylingPrompt.DiscardOnBack
        } else {
            onNavigateBack()
        }
    }

    fun requestSave() {
        if (!isDirty) {
            onNavigateBack()
            return
        }
        prompt = UiStylingPrompt.ConfirmSave
    }

    BackHandler(onBack = ::requestBack)

    when (prompt) {
        UiStylingPrompt.DiscardOnBack -> {
            StyleConfirmDialog(
                title = "Discard changes?",
                confirmLabel = "Discard",
                dismissLabel = "Continue",
                onConfirm = {
                    prompt = UiStylingPrompt.None
                    viewModel.discard()
                    onNavigateBack()
                },
                onDismiss = { prompt = UiStylingPrompt.None },
            )
        }
        UiStylingPrompt.ConfirmSave -> {
            StyleConfirmDialog(
                title = "Save your changes?",
                confirmLabel = "Yes",
                dismissLabel = "No",
                onConfirm = {
                    prompt = UiStylingPrompt.None
                    viewModel.save(onSaved = onNavigateBack)
                },
                onDismiss = {
                    // "No" - leave without saving
                    prompt = UiStylingPrompt.None
                    viewModel.discard()
                    onNavigateBack()
                },
                onCancel = { prompt = UiStylingPrompt.None },
            )
        }
        UiStylingPrompt.None -> Unit
    }

    if (!isReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GrooveTheme.colors.canvas),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = GrooveTheme.colors.accent)
        }
        return
    }

    GrooveScreen(
        title = "UI styling",
        onBackClick = ::requestBack,
        contentPadding = PaddingValues.Zero,
        actions = {
            TextButton(
                onClick = ::requestSave,
                enabled = isDirty,
            ) {
                Text(
                    text = "Save",
                    style = GrooveTheme.typography.buttonLabel.toTextStyle(),
                    color = if (isDirty) {
                        GrooveTheme.colors.onSurface
                    } else {
                        GrooveTheme.colors.muted.copy(alpha = 0.35f)
                    },
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GrooveTheme.spacing.l)
                .padding(
                    bottom = GrooveTheme.spacing.l +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
        ) {
            Spacer(Modifier.height(GrooveTheme.spacing.s))
            LiveStylePreview(style = draft)
            Spacer(Modifier.height(GrooveTheme.spacing.l))

            EditorSectionTitle("Presets")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(GrooveTheme.spacing.s),
            ) {
                GrooveStyleCatalog.all.forEach { preset ->
                    PresetChip(
                        label = preset.displayName,
                        selected = isPresetSelected(draft, preset),
                        onClick = { viewModel.applyPreset(preset) },
                    )
                }
            }

            Spacer(Modifier.height(GrooveTheme.spacing.l))
            EditorSectionTitle("Colors")
            ColorEditor(
                label = "Background (canvas)",
                color = draft.colors.canvas,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(canvas = c)) }
                },
            )
            ColorEditor(
                label = "Edge gradient",
                color = draft.colors.edgeGradient,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(edgeGradient = c)) }
                },
            )
            ColorEditor(
                label = "Surface (cards)",
                color = draft.colors.surface,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(surface = c)) }
                },
            )
            ColorEditor(
                label = "Raised surface",
                color = draft.colors.surfaceRaised,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(surfaceRaised = c)) }
                },
            )
            ColorEditor(
                label = "Primary text",
                color = draft.colors.onSurface,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(onSurface = c)) }
                },
            )
            ColorEditor(
                label = "Secondary text",
                color = draft.colors.muted,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(muted = c)) }
                },
            )
            ColorEditor(
                label = "Accent / buttons",
                color = draft.colors.accent,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(accent = c)) }
                },
            )
            ColorEditor(
                label = "On accent",
                color = draft.colors.onAccent,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(onAccent = c)) }
                },
            )
            ColorEditor(
                label = "Inactive",
                color = draft.colors.inactive,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(inactive = c)) }
                },
            )
            ColorEditor(
                label = "Inactive container",
                color = draft.colors.inactiveContainer,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(inactiveContainer = c)) }
                },
            )
            ColorEditor(
                label = "Brand secondary",
                color = draft.colors.brandSecondary,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(brandSecondary = c)) }
                },
            )
            ColorEditor(
                label = "Brand tertiary",
                color = draft.colors.brandTertiary,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(brandTertiary = c)) }
                },
            )
            ColorEditor(
                label = "Warning",
                color = draft.colors.warning,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(warning = c)) }
                },
            )
            ColorEditor(
                label = "Error",
                color = draft.colors.error,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(error = c)) }
                },
            )
            ColorEditor(
                label = "Slider track",
                color = draft.colors.sliderTrack,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(sliderTrack = c)) }
                },
            )
            ColorEditor(
                label = "Slider fill",
                color = draft.colors.sliderFill,
                onColor = { c ->
                    viewModel.updateDraft { it.copy(colors = it.colors.copy(sliderFill = c)) }
                },
            )

            Spacer(Modifier.height(GrooveTheme.spacing.l))
            EditorSectionTitle("Spacing & padding")
            DpSlider(
                label = "Extra small",
                value = draft.spacing.xs,
                range = 4f..16f,
                onChange = { v ->
                    viewModel.updateDraft {
                        it.copy(spacing = it.spacing.copy(xs = v))
                    }
                },
            )
            DpSlider(
                label = "Small",
                value = draft.spacing.s,
                range = 8f..20f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(spacing = it.spacing.copy(s = v)) }
                },
            )
            DpSlider(
                label = "Medium / card padding",
                value = draft.spacing.m,
                range = 10f..28f,
                onChange = { v ->
                    viewModel.updateDraft {
                        it.copy(spacing = it.spacing.copy(m = v, cardPadding = v))
                    }
                },
            )
            DpSlider(
                label = "Large",
                value = draft.spacing.l,
                range = 16f..40f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(spacing = it.spacing.copy(l = v)) }
                },
            )
            DpSlider(
                label = "List item gap",
                value = draft.spacing.listItemSpacing,
                range = 4f..24f,
                onChange = { v ->
                    viewModel.updateDraft {
                        it.copy(spacing = it.spacing.copy(listItemSpacing = v))
                    }
                },
            )
            DpSlider(
                label = "Button min height",
                value = draft.spacing.buttonMinHeight,
                range = 40f..64f,
                onChange = { v ->
                    viewModel.updateDraft {
                        it.copy(spacing = it.spacing.copy(buttonMinHeight = v))
                    }
                },
            )
            DpSlider(
                label = "App bar height",
                value = draft.spacing.appBarHeight,
                range = 56f..96f,
                onChange = { v ->
                    viewModel.updateDraft {
                        it.copy(spacing = it.spacing.copy(appBarHeight = v))
                    }
                },
            )

            Spacer(Modifier.height(GrooveTheme.spacing.l))
            EditorSectionTitle("Corners")
            DpSlider(
                label = "Card radius",
                value = draft.radii.card,
                range = 0f..28f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(radii = it.radii.copy(card = v)) }
                },
            )
            DpSlider(
                label = "Button radius",
                value = draft.radii.button,
                range = 0f..24f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(radii = it.radii.copy(button = v)) }
                },
            )
            DpSlider(
                label = "Artwork radius",
                value = draft.radii.artwork,
                range = 0f..28f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(radii = it.radii.copy(artwork = v)) }
                },
            )

            Spacer(Modifier.height(GrooveTheme.spacing.l))
            EditorSectionTitle("Icon sizes")
            DpSlider(
                label = "Small icon",
                value = draft.iconSizes.sm,
                range = 14f..24f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(iconSizes = it.iconSizes.copy(sm = v)) }
                },
            )
            DpSlider(
                label = "Medium icon",
                value = draft.iconSizes.md,
                range = 18f..32f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(iconSizes = it.iconSizes.copy(md = v)) }
                },
            )
            DpSlider(
                label = "Large icon",
                value = draft.iconSizes.lg,
                range = 22f..40f,
                onChange = { v ->
                    viewModel.updateDraft { it.copy(iconSizes = it.iconSizes.copy(lg = v)) }
                },
            )

            Spacer(modifier = Modifier.height(GrooveTheme.spacing.l))
            EditorSectionTitle("Typography")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(GrooveTheme.spacing.s),
            ) {
                GrooveTypographyScale.entries.forEach { scale ->
                    PresetChip(
                        label = scale.displayName,
                        selected = GrooveTypographyScale.matching(draft.typography) == scale,
                        onClick = {
                            viewModel.updateDraft { it.copy(typography = scale.roles) }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(GrooveTheme.spacing.s))
            Text(
                text = when (GrooveTypographyScale.matching(draft.typography)) {
                    GrooveTypographyScale.Small -> "Compact - closer to Material title/body small"
                    GrooveTypographyScale.Medium -> "Default - Material 3 baseline for phone music UI"
                    GrooveTypographyScale.Large -> "Roomier - one Major Second step up from Medium"
                    GrooveTypographyScale.ExtraLarge -> "Largest - accessibility-friendly reading sizes"
                    null -> "Pick a size preset"
                },
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = GrooveTheme.colors.muted.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(GrooveTheme.spacing.l))
        }
    }
}

private fun isCustomDraft(draft: GrooveStyle): Boolean =
    draft.id == GrooveStyleIds.CUSTOM

private fun isPresetSelected(draft: GrooveStyle, preset: GrooveStyle): Boolean {
    if (isCustomDraft(draft) || draft.id != preset.id) return false
    return GrooveStyleCatalog.all.any { catalog ->
        catalog.id == preset.id &&
            GrooveStyleCodec.encode(catalog) == GrooveStyleCodec.encode(draft)
    }
}

@Composable
private fun StyleConfirmDialog(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val colors = GrooveTheme.colors
    AlertDialog(
        onDismissRequest = { (onCancel ?: onDismiss)() },
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.muted.copy(alpha = 0.85f),
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = colors.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = colors.muted.copy(alpha = 0.75f))
            }
        },
    )
}

@Composable
private fun LiveStylePreview(style: GrooveStyle) {
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
private fun EditorSectionTitle(text: String) {
    Text(
        text = text,
        style = GrooveTheme.typography.sectionTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
        modifier = Modifier.padding(bottom = GrooveTheme.spacing.s),
    )
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun ColorEditor(
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

@Composable
private fun DpSlider(
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
