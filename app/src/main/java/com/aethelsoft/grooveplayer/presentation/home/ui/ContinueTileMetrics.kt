package com.aethelsoft.grooveplayer.presentation.home.ui

import com.aethelsoft.grooveplayer.utils.DeviceType

/** Phone "Where you left off" tiles across the library content width (about a quarter each). */
const val CONTINUE_TILES_ACROSS = 4

/** Title lines reserved on a phone continue tile so song names stay readable. */
const val CONTINUE_TITLE_MAX_LINES = 2

/** Artist line under the title on a phone continue tile. */
const val CONTINUE_SUBTITLE_MAX_LINES = 1

/**
 * Pre-#16 continue tiles divided the screen like this:
 * `(screenWidth - [LEGACY_CONTINUE_TILE_DIVISOR] * spacing) / [LEGACY_CONTINUE_TILE_DIVISOR]`.
 * Tablet and large tablet still use that side length, and the card stays square.
 */
const val LEGACY_CONTINUE_TILE_DIVISOR = 8

/** Pre-#16 cards drew a single ellipsized title inside the square. */
const val LEGACY_CONTINUE_TITLE_MAX_LINES = 1

/** Pre-#16 cards drew a single ellipsized subtitle inside the square. */
const val LEGACY_CONTINUE_SUBTITLE_MAX_LINES = 1

/**
 * Width and height of one "Where you left off" tile for [deviceType].
 *
 * Phone uses [CONTINUE_TILES_ACROSS] of the library content width and may grow
 * taller than wide so two title lines fit. Tablet and large tablet use the
 * pre-#16 screen-width square: `(screenWidthDp - divisor * gapDp) / divisor`.
 */
data class ContinueTileSpec(
    val widthDp: Float,
    val heightDp: Float,
    val titleMaxLines: Int,
    val subtitleMaxLines: Int,
    val square: Boolean,
)

/**
 * Width of one phone continue tile so [tilesAcross] tiles, plus the gaps between them,
 * fill [contentWidthDp] (the library grid's inner width, already inside page padding).
 */
fun continueTileWidthDp(
    contentWidthDp: Float,
    gapDp: Float,
    tilesAcross: Int = CONTINUE_TILES_ACROSS,
): Float {
    if (contentWidthDp <= 0f || tilesAcross <= 0) return 0f
    val gaps = gapDp.coerceAtLeast(0f) * (tilesAcross - 1)
    return ((contentWidthDp - gaps) / tilesAcross).coerceAtLeast(0f)
}

/**
 * Side of a pre-#16 continue tile. The horizontal grid was this tall and each card
 * was square (`aspectRatio(1)`), sized from the full window width rather than the
 * padded library content width.
 */
fun legacyContinueTileSideDp(
    screenWidthDp: Float,
    gapDp: Float,
): Float {
    if (screenWidthDp <= 0f) return 0f
    val spacing = gapDp.coerceAtLeast(0f) * LEGACY_CONTINUE_TILE_DIVISOR
    return ((screenWidthDp - spacing) / LEGACY_CONTINUE_TILE_DIVISOR).coerceAtLeast(0f)
}

/**
 * Square when [tileWidthDp] already fits the label block; otherwise tall enough
 * for [titleLines] of the card title and [subtitleLines] of the card subtitle
 * inside [overlayPaddingDp]. Phone tiles only; tablet squares do not grow.
 */
fun continueTileHeightDp(
    tileWidthDp: Float,
    overlayPaddingDp: Float,
    titleLineHeightDp: Float,
    subtitleLineHeightDp: Float,
    titleLines: Int = CONTINUE_TITLE_MAX_LINES,
    subtitleLines: Int = CONTINUE_SUBTITLE_MAX_LINES,
): Float {
    if (tileWidthDp <= 0f) return 0f
    val labelBlock = overlayPaddingDp.coerceAtLeast(0f) * 2f +
        titleLineHeightDp.coerceAtLeast(0f) * titleLines.coerceAtLeast(0) +
        subtitleLineHeightDp.coerceAtLeast(0f) * subtitleLines.coerceAtLeast(0)
    return maxOf(tileWidthDp, labelBlock)
}

/**
 * Continue-tile size for the app's window class.
 * [DeviceType] is the gate. A wide phone stays four-across, and a narrow
 * tablet still uses the legacy screen-width square.
 */
fun continueTileSpec(
    deviceType: DeviceType,
    contentWidthDp: Float,
    screenWidthDp: Float,
    gapDp: Float,
    overlayPaddingDp: Float,
    titleLineHeightDp: Float,
    subtitleLineHeightDp: Float,
): ContinueTileSpec = when (deviceType) {
    DeviceType.PHONE -> {
        val width = continueTileWidthDp(contentWidthDp, gapDp, CONTINUE_TILES_ACROSS)
        ContinueTileSpec(
            widthDp = width,
            heightDp = continueTileHeightDp(
                tileWidthDp = width,
                overlayPaddingDp = overlayPaddingDp,
                titleLineHeightDp = titleLineHeightDp,
                subtitleLineHeightDp = subtitleLineHeightDp,
            ),
            titleMaxLines = CONTINUE_TITLE_MAX_LINES,
            subtitleMaxLines = CONTINUE_SUBTITLE_MAX_LINES,
            square = false,
        )
    }
    DeviceType.TABLET,
    DeviceType.LARGE_TABLET -> {
        val side = legacyContinueTileSideDp(screenWidthDp, gapDp)
        ContinueTileSpec(
            widthDp = side,
            heightDp = side,
            titleMaxLines = LEGACY_CONTINUE_TITLE_MAX_LINES,
            subtitleMaxLines = LEGACY_CONTINUE_SUBTITLE_MAX_LINES,
            square = true,
        )
    }
}
