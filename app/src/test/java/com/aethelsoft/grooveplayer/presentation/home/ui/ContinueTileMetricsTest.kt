package com.aethelsoft.grooveplayer.presentation.home.ui

import com.aethelsoft.grooveplayer.utils.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContinueTileMetricsTest {
    @Test
    fun `four tiles and three gaps fill the content width`() {
        val contentWidth = 328f
        val gap = 12f
        val tile = continueTileWidthDp(contentWidth, gap)

        assertEquals(73f, tile, 0.001f)
        assertEquals(contentWidth, tile * CONTINUE_TILES_ACROSS + gap * 3, 0.001f)
    }

    @Test
    fun `quarter of a phone content width is about four across`() {
        // 393dp window minus 16dp page padding on each side.
        val contentWidth = 393f - 32f
        val tile = continueTileWidthDp(contentWidth, gapDp = 12f)

        assertEquals(contentWidth / 4f, tile + 12f * 3f / 4f, 0.001f)
        assertEquals(4, CONTINUE_TILES_ACROSS)
    }

    @Test
    fun `phone layout is four across with two title lines`() {
        val screenWidth = 393f
        val contentWidth = screenWidth - 32f
        val gap = 12f
        val spec = phoneSpec(contentWidth, screenWidth, gap)

        val fourAcross = (contentWidth - gap * (CONTINUE_TILES_ACROSS - 1)) / CONTINUE_TILES_ACROSS
        assertEquals(fourAcross, spec.widthDp, 0.001f)
        assertEquals(12f * 2 + 24f * 2 + 20f, spec.heightDp, 0.001f)
        assertEquals(CONTINUE_TITLE_MAX_LINES, spec.titleMaxLines)
        assertEquals(CONTINUE_SUBTITLE_MAX_LINES, spec.subtitleMaxLines)
        assertEquals(false, spec.square)
        assertNotEquals(legacySide(screenWidth, gap), spec.widthDp, 0.001f)
    }

    @Test
    fun `phone stays four across on a wide content width`() {
        val contentWidth = 1200f
        val gap = 12f
        val spec = phoneSpec(contentWidth, screenWidthDp = 1232f, gapDp = gap)

        assertEquals(
            (contentWidth - gap * 3) / CONTINUE_TILES_ACROSS,
            spec.widthDp,
            0.001f,
        )
        assertEquals(4, CONTINUE_TILES_ACROSS)
    }

    @Test
    fun `tablet uses the pre-16 screen square not four across`() {
        // 600dp is the tablet breakpoint. Content width is inside 16dp page padding.
        val screenWidth = 600f
        val contentWidth = screenWidth - 32f
        val gap = 12f
        val spec = continueTileSpec(
            deviceType = DeviceType.TABLET,
            contentWidthDp = contentWidth,
            screenWidthDp = screenWidth,
            gapDp = gap,
            overlayPaddingDp = 12f,
            titleLineHeightDp = 24f,
            subtitleLineHeightDp = 20f,
        )

        val legacy = legacySide(screenWidth, gap)
        val fourAcross = (contentWidth - gap * 3) / CONTINUE_TILES_ACROSS
        assertEquals(legacy, spec.widthDp, 0.001f)
        assertEquals(legacy, spec.heightDp, 0.001f)
        assertEquals(true, spec.square)
        assertEquals(LEGACY_CONTINUE_TITLE_MAX_LINES, spec.titleMaxLines)
        assertEquals(LEGACY_CONTINUE_SUBTITLE_MAX_LINES, spec.subtitleMaxLines)
        assertNotEquals(fourAcross, spec.widthDp, 0.001f)
        // Label block is taller than the restored square; height must not grow.
        assertEquals(63f, spec.heightDp, 0.001f)
    }

    @Test
    fun `large tablet uses the same pre-16 square as tablet`() {
        val screenWidth = 1280f
        val contentWidth = screenWidth - 32f
        val gap = 12f
        val spec = continueTileSpec(
            deviceType = DeviceType.LARGE_TABLET,
            contentWidthDp = contentWidth,
            screenWidthDp = screenWidth,
            gapDp = gap,
            overlayPaddingDp = 12f,
            titleLineHeightDp = 80f,
            subtitleLineHeightDp = 40f,
        )

        val legacy = legacySide(screenWidth, gap)
        val fourAcross = (contentWidth - gap * 3) / CONTINUE_TILES_ACROSS
        assertEquals(legacy, spec.widthDp, 0.001f)
        assertEquals(legacy, spec.heightDp, 0.001f)
        assertEquals(true, spec.square)
        assertEquals(1, spec.titleMaxLines)
        assertEquals(1, spec.subtitleMaxLines)
        assertNotEquals(fourAcross, spec.widthDp, 0.001f)
        // A tall label block must not stretch the restored square.
        val grown = continueTileHeightDp(
            tileWidthDp = legacy,
            overlayPaddingDp = 12f,
            titleLineHeightDp = 80f,
            subtitleLineHeightDp = 40f,
        )
        assertNotEquals(grown, spec.heightDp, 0.001f)
    }

    @Test
    fun `device type selects the formula even when the widths match a phone`() {
        val screenWidth = 393f
        val contentWidth = screenWidth - 32f
        val gap = 12f
        val tablet = continueTileSpec(
            deviceType = DeviceType.TABLET,
            contentWidthDp = contentWidth,
            screenWidthDp = screenWidth,
            gapDp = gap,
            overlayPaddingDp = 12f,
            titleLineHeightDp = 24f,
            subtitleLineHeightDp = 20f,
        )

        assertEquals(legacySide(screenWidth, gap), tablet.widthDp, 0.001f)
        assertNotEquals(phoneSpec(contentWidth, screenWidth, gap).widthDp, tablet.widthDp, 0.001f)
    }

    @Test
    fun `legacy side is screen width minus eight spacings over eight`() {
        assertEquals((800f - 12f * 8) / 8f, legacyContinueTileSideDp(800f, 12f), 0.001f)
        assertEquals(0f, legacyContinueTileSideDp(0f, 12f), 0.001f)
        assertEquals(0f, legacyContinueTileSideDp(-20f, 12f), 0.001f)
    }

    @Test
    fun `tile grows when the square is shorter than the label block`() {
        val height = continueTileHeightDp(
            tileWidthDp = 73f,
            overlayPaddingDp = 12f,
            titleLineHeightDp = 24f,
            subtitleLineHeightDp = 20f,
        )

        assertEquals(12f * 2 + 24f * 2 + 20f, height, 0.001f)
    }

    @Test
    fun `tile stays square when it already fits two title lines`() {
        assertEquals(
            160f,
            continueTileHeightDp(
                tileWidthDp = 160f,
                overlayPaddingDp = 12f,
                titleLineHeightDp = 24f,
                subtitleLineHeightDp = 20f,
            ),
            0.001f,
        )
    }

    @Test
    fun `non positive content width yields no tile`() {
        assertEquals(0f, continueTileWidthDp(0f, 12f), 0.001f)
        assertEquals(0f, continueTileWidthDp(-20f, 12f), 0.001f)
    }

    private fun legacySide(screenWidthDp: Float, gapDp: Float): Float =
        (screenWidthDp - gapDp * LEGACY_CONTINUE_TILE_DIVISOR) / LEGACY_CONTINUE_TILE_DIVISOR

    private fun phoneSpec(contentWidthDp: Float, screenWidthDp: Float, gapDp: Float) =
        continueTileSpec(
            deviceType = DeviceType.PHONE,
            contentWidthDp = contentWidthDp,
            screenWidthDp = screenWidthDp,
            gapDp = gapDp,
            overlayPaddingDp = 12f,
            titleLineHeightDp = 24f,
            subtitleLineHeightDp = 20f,
        )
}
