package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

/**
 * Approximate bottom mini-player content height (excluding [navigationBarsInset]).
 * Matches the clearance used by home grids so list content clears the overlay bar.
 */
val MiniPlayerClearance: Dp = 106.dp

/**
 * Approximate top-anchored mini-player content height (excluding [statusBarsInset]),
 * used when song-details is expanded over the full player.
 */
val TopMiniPlayerClearance: Dp = 128.dp

/**
 * Shared top-bar metrics used by [XAppBar], [GradientAppBar], and Player chrome.
 *
 * Total chrome height = status bar inset + [APP_BAR_HEIGHT].
 * Horizontal edge padding is [M_PADDING] on all devices so Profile / Home / Player align.
 */
@Composable
fun topBarHorizontalPadding(deviceType: DeviceType) = when (deviceType) {
    DeviceType.PHONE,
    DeviceType.TABLET,
    DeviceType.LARGE_TABLET -> M_PADDING
}

/** Status-bar (top system) inset. */
@Composable
fun statusBarsInset(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** Navigation / gesture bar (bottom system) inset. */
@Composable
fun navigationBarsInset(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Bottom content clearance for edge-to-edge screens.
 * Always includes the nav/gesture bar; optionally clears the floating mini player.
 */
@Composable
fun grooveBottomContentInset(includeMiniPlayer: Boolean = false): Dp =
    navigationBarsInset() + if (includeMiniPlayer) MiniPlayerClearance else 0.dp

/** Top inset for content sitting below a top-anchored mini player overlay. */
@Composable
fun topAnchoredMiniPlayerInset(): Dp = statusBarsInset() + TopMiniPlayerClearance

/**
 * Whether bottom content should clear the floating mini player
 * (song active and full player not covering the bottom bar).
 */
@Composable
fun rememberClearMiniPlayer(): Boolean {
    val playerViewModel = LocalPlayerViewModel.current ?: return false
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isFullScreenPlayerOpened by playerViewModel.isFullScreenPlayerOpened.collectAsState()
    return currentSong != null && !isFullScreenPlayerOpened
}

/** Content offset below a groove top bar: status inset + style app-bar height. */
@Composable
fun topBarContentInset(): Dp =
    GrooveTheme.spacing.appBarHeight + statusBarsInset()

@Composable
fun Modifier.grooveTopBarContainer(deviceType: DeviceType): Modifier {
    val barHeight = GrooveTheme.spacing.appBarHeight
    // Status bars ∪ display cutout for notched / landscape devices.
    val topChromeInsets = WindowInsets.statusBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    return this
        .fillMaxWidth()
        .grooveTopChromeGlass()
        .windowInsetsPadding(topChromeInsets)
        .height(barHeight)
        .padding(end = topBarHorizontalPadding(deviceType))
}

@Composable
fun GradientAppBar(
    title: String,
    deviceType: DeviceType,
    modifier: Modifier = Modifier,
    /** Override back action (e.g. close drawer). When null, uses navigation.goBack(). */
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigation = rememberNavigationActions()

    Box(
        modifier = modifier.grooveTopBarContainer(deviceType),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { (onBackClick ?: { navigation.goBack() })() }
            ) {
                Icon(
                    imageVector = XBack,
                    contentDescription = "Back",
                    tint = GrooveTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.size(S_PADDING))
            Text(
                text = title,
                style = GrooveTheme.typography.pageTitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}
