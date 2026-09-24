package com.aethelsoft.grooveplayer.presentation.ui_customisation

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
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.ui_customisation.ui.ColorEditor
import com.aethelsoft.grooveplayer.presentation.ui_customisation.ui.CustomisationConfirmDialog
import com.aethelsoft.grooveplayer.presentation.ui_customisation.ui.DpSlider
import com.aethelsoft.grooveplayer.presentation.ui_customisation.ui.EditorSectionTitle
import com.aethelsoft.grooveplayer.presentation.ui_customisation.ui.LiveCustomisationPreview
import com.aethelsoft.grooveplayer.presentation.ui_customisation.ui.PresetChip
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
fun UiCustomisationScreen(
    onNavigateBack: () -> Unit,
    viewModel: UiCustomisationViewModel = hiltViewModel(),
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
            CustomisationConfirmDialog(
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
            CustomisationConfirmDialog(
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
                    // Inside the scroll content: rests below the app bar,
                    // scrolls underneath the translucent glass.
                    top = topBarContentInset(),
                    bottom = GrooveTheme.spacing.l +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
        ) {
            Spacer(Modifier.height(GrooveTheme.spacing.s))
            LiveCustomisationPreview(style = draft)
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
