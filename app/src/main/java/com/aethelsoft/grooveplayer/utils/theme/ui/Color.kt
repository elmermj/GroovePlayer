package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware color aliases used across Compose UI.
 *
 * These resolve from [LocalGrooveStyle] so UI styling presets / overrides apply app-wide.
 * Prefer [GrooveTheme.colors] in new code; keep these for gradual migration of existing call sites.
 *
 * For non-Compose / default-parameter fallbacks, use [GrooveStyleCatalog.Default].colors.
 */
val GrooveBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.canvas

val SoftWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.muted

val SoftBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.surface

val InactivePrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.inactive

val InactiveSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.inactiveContainer

val HighlightPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.surfaceRaised

val brandPrimaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.inactive

val brandSecondaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.brandSecondary

val brandTertiaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.brandTertiary

val volumeWarningColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.warning

val volumeMaxColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.error

val sliderSpaceColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.sliderTrack

val sliderFilledColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.sliderFill

val buttonColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.colors.sliderFill
