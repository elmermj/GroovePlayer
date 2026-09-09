package com.aethelsoft.grooveplayer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import com.aethelsoft.grooveplayer.utils.theme.ui.LocalGrooveStyle

/**
 * Theme-aware spacing aliases. Resolve from [LocalGrooveStyle] so UI styling applies app-wide.
 *
 * Default-parameter values cannot call these (not a constant expression) — use
 * [DefaultAppBarHeight] / [DefaultLPadding] / … there instead.
 */
val APP_BAR_HEIGHT: Dp
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.spacing.appBarHeight

val L_PADDING: Dp
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.spacing.l

val M_PADDING: Dp
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.spacing.m

val S_PADDING: Dp
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.spacing.s

val XS_PADDING: Dp
    @Composable
    @ReadOnlyComposable
    get() = LocalGrooveStyle.current.spacing.xs

/** Static fallbacks for default function parameters (mirrors Default style). */
val DefaultAppBarHeight: Dp get() = com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog.Default.spacing.appBarHeight
val DefaultLPadding: Dp get() = com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog.Default.spacing.l
val DefaultMPadding: Dp get() = com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog.Default.spacing.m
val DefaultSPadding: Dp get() = com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog.Default.spacing.s
val DefaultXsPadding: Dp get() = com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog.Default.spacing.xs
