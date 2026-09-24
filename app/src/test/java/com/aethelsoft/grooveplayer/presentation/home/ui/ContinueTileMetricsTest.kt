package com.aethelsoft.grooveplayer.presentation.home.ui

import org.junit.Assert.assertEquals
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
}
