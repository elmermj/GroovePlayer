package com.aethelsoft.grooveplayer.presentation.home.ui

/** "Where you left off" tiles across the library content width (about a quarter each). */
const val CONTINUE_TILES_ACROSS = 4

/** Title lines reserved on a continue tile so song names stay readable. */
const val CONTINUE_TITLE_MAX_LINES = 2

/** Artist line under the title. */
const val CONTINUE_SUBTITLE_MAX_LINES = 1

/**
 * Width of one continue tile so [tilesAcross] tiles, plus the gaps between them,
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
 * Square when [tileWidthDp] already fits the label block; otherwise tall enough
 * for [titleLines] of the card title and [subtitleLines] of the card subtitle
 * inside [overlayPaddingDp].
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
