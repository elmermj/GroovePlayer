package com.aethelsoft.grooveplayer.utils

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize

/** The app's three existing layout families, selected from current window width. */
enum class DeviceType {
    PHONE,
    TABLET,
    LARGE_TABLET,
}

/** Existing GroovePlayer breakpoints, centralized for every route and orientation policy. */
const val TABLET_MIN_WIDTH_DP = 600f
const val LARGE_TABLET_MIN_WIDTH_DP = 840f

/** A live snapshot of the current app window. */
data class AdaptiveWindowInfo(
    val widthDp: Float,
    val heightDp: Float,
    val deviceType: DeviceType,
    /** Pixel container size for overlays/effects that still need IntSize. */
    val containerSizePx: IntSize,
)

/** Pure threshold mapping kept separate for focused unit tests. */
fun deviceTypeForWidth(widthDp: Float): DeviceType = when {
    widthDp < TABLET_MIN_WIDTH_DP -> DeviceType.PHONE
    widthDp < LARGE_TABLET_MIN_WIDTH_DP -> DeviceType.TABLET
    else -> DeviceType.LARGE_TABLET
}

/** Compact windows lock portrait; tablet-sized windows leave orientation unlocked. */
fun requestedOrientationFor(deviceType: DeviceType): Int = when (deviceType) {
    DeviceType.PHONE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    DeviceType.TABLET,
    DeviceType.LARGE_TABLET -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

/**
 * Root-provided live window snapshot. A nullable default lets previews and isolated composables
 * fall back to their own current [LocalWindowInfo] without inventing a second policy.
 */
val LocalAdaptiveWindowInfo = compositionLocalOf<AdaptiveWindowInfo?> { null }

/**
 * Returns the current adaptive window class from the Compose window container bounds.
 *
 * [LocalWindowInfo] changes when a foldable folds/unfolds, a freeform window is resized, or
 * `wm size` changes. [LocalDensity] changes for `wm density`; converting pixels with that live
 * density keeps classification based on the current window width in dp rather than physical panel
 * resolution or device/model identity.
 */
@Composable
fun rememberAdaptiveWindowInfo(): AdaptiveWindowInfo {
    LocalAdaptiveWindowInfo.current?.let { return it }

    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current.density
    return remember(containerSize.width, containerSize.height, density) {
        val widthDp = containerSize.width / density
        val heightDp = containerSize.height / density
        AdaptiveWindowInfo(
            widthDp = widthDp,
            heightDp = heightDp,
            deviceType = deviceTypeForWidth(widthDp),
            containerSizePx = containerSize,
        )
    }
}

/** Compatibility name used throughout the existing layouts; now backed by live window metrics. */
@Composable
fun rememberDeviceType(): DeviceType = rememberAdaptiveWindowInfo().deviceType
