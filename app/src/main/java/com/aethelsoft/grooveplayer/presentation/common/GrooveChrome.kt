package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

/**
 * Dark Groove page chrome: canvas + [GradientAppBar] overlay + content inset.
 * Colors / radii / spacing come from [GrooveTheme] (user-configurable).
 *
 * Default padding clears the navigation/gesture bar and, when a song is active,
 * the floating mini player. Pass [contentPadding] to override.
 *
 * For scrollable lists, prefer [contentPadding] = [PaddingValues.Zero] and put
 * horizontal/vertical padding on the LazyColumn contentPadding instead
 * (include [grooveBottomContentInset] in that contentPadding).
 */
@Composable
fun GrooveScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val deviceType = rememberDeviceType()
    val colors = GrooveTheme.colors
    val spacing = GrooveTheme.spacing
    val resolvedPadding = contentPadding ?: PaddingValues(
        start = spacing.l,
        end = spacing.l,
        bottom = spacing.l + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarContentInset())
                .padding(resolvedPadding),
            content = content,
        )
        GradientAppBar(
            title = title,
            deviceType = deviceType,
            onBackClick = onBackClick,
            actions = actions,
        )
    }
}

/**
 * Surface card used for share options, device rows, banners, etc.
 */
@Composable
fun GrooveSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GrooveTheme.colors
    val radii = GrooveTheme.radii
    val resolvedPadding = contentPadding ?: PaddingValues(GrooveTheme.spacing.m)
    val clickMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.cardShape)
            .background(colors.surface)
            .then(clickMod)
            .padding(resolvedPadding),
        content = content,
    )
}

@Composable
fun GrooveSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = GrooveTheme.colors.onSurface,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Accent / surface action button pair used across share confirmation & approval.
 */
@Composable
fun GrooveActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = GrooveTheme.colors
    val radii = GrooveTheme.radii
    val spacing = GrooveTheme.spacing
    val container = when {
        !enabled -> colors.surface
        isPrimary -> colors.accent
        else -> colors.surface
    }
    val content = when {
        !enabled -> colors.muted.copy(alpha = 0.35f)
        isPrimary -> colors.onAccent
        else -> colors.muted
    }
    Box(
        modifier = modifier
            .heightIn(min = spacing.buttonMinHeight)
            .clip(radii.buttonShape)
            .background(container)
            .then(
                if (!isPrimary && enabled) {
                    Modifier.border(1.dp, colors.muted.copy(alpha = 0.35f), radii.buttonShape)
                } else {
                    Modifier
                }
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = spacing.m, vertical = spacing.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = GrooveTheme.typography.buttonLabel.toTextStyle(),
            color = content,
        )
    }
}

@Composable
fun GrooveActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GrooveTheme.spacing.m),
    ) {
        GrooveActionButton(
            label = secondaryLabel,
            onClick = onSecondary,
            isPrimary = false,
            modifier = Modifier.weight(1f),
        )
        GrooveActionButton(
            label = primaryLabel,
            onClick = onPrimary,
            isPrimary = true,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun GrooveCardTitle(text: String) {
    Text(
        text = text,
        style = GrooveTheme.typography.cardTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )
}

@Composable
fun GrooveCardSubtitle(text: String) {
    Text(
        text = text,
        style = GrooveTheme.typography.cardSubtitle.toTextStyle(),
        color = GrooveTheme.colors.muted.copy(alpha = 0.7f),
    )
}

@Composable
fun GrooveMutedText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = GrooveTheme.typography.body.toTextStyle(),
) {
    Text(
        text = text,
        style = style,
        color = GrooveTheme.colors.muted.copy(alpha = 0.65f),
        modifier = modifier,
    )
}

/** Small vertical gap helper matching share card subtitle spacing. */
@Composable
fun GrooveTinySpacer() {
    Spacer(modifier = Modifier.height(GrooveTheme.spacing.xs / 2))
}
