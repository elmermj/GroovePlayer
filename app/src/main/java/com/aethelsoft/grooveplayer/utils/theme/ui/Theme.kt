package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Provides [LocalGrooveStyle] and a Material [ColorScheme] / [Typography] derived from it.
 *
 * Pass a resolved [GrooveStyle] from user settings (see [GrooveStyleCatalog.resolve])
 * so a customization screen can live-update the whole app.
 *
 * [darkTheme] / [dynamicColor] are ignored — Groove is style-driven, not system-light.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun GroovePlayerTheme(
    style: GrooveStyle = GrooveStyleCatalog.Default,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGrooveStyle provides style) {
        MaterialTheme(
            colorScheme = style.toColorScheme(),
            typography = style.typography.toMaterialTypography(),
            content = content,
        )
    }
}
