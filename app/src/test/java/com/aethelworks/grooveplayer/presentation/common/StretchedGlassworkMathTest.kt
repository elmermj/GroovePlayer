package com.aethelworks.grooveplayer.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchedGlassworkMathTest {
    @Test
    fun centerSquareCrop_wideBitmap_usesTheCenterHeight() {
        val crop = centerSquareCrop(width = 200, height = 80)

        assertEquals(60, crop!!.left)
        assertEquals(0, crop.top)
        assertEquals(80, crop.size)
        assertEquals(139, crop.edgeX)
    }

    @Test
    fun centerSquareCrop_tallBitmap_usesTheCenterWidth() {
        val crop = centerSquareCrop(width = 50, height = 90)

        assertEquals(0, crop!!.left)
        assertEquals(20, crop.top)
        assertEquals(50, crop.size)
        assertEquals(49, crop.edgeX)
    }

    @Test
    fun centerSquareCrop_rejectsEmptyBitmaps() {
        assertNull(centerSquareCrop(0, 10))
        assertNull(centerSquareCrop(10, 0))
    }

    @Test
    fun modelKey_ignoresBlankArtwork() {
        assertNull(stretchedGlassworkModelKey(null))
        assertNull(stretchedGlassworkModelKey("   "))
        assertEquals("cover.jpg", stretchedGlassworkModelKey(" cover.jpg "))
    }

    @Test
    fun scrim_isHeavierForPaleArtAndForUnblurredRows() {
        val paleBlurred = scrimAlphaFor(luminance = 0.9f, blurred = true)
        val warmBlurred = scrimAlphaFor(luminance = 0.2f, blurred = true)
        val unblurred = scrimAlphaFor(luminance = 0.2f, blurred = false)

        assertTrue(paleBlurred > warmBlurred)
        assertTrue(unblurred > warmBlurred)
        assertEquals(0.36f, warmBlurred, 0.001f)
        assertEquals(0.62f, unblurred, 0.001f)
    }

    @Test
    fun boxBlur_mixesVerticalColorBands() {
        val width = 4
        val height = 4
        val orange = 0xFFFF8800.toInt()
        val blue = 0xFF2244AA.toInt()
        val pixels = IntArray(width * height) { index ->
            if (index / width < 2) orange else blue
        }

        val blurred = boxBlur(pixels, width, height, radius = 2)
        val mixedRed = (blurred[1 * width] ushr 16) and 0xFF

        assertTrue(mixedRed in 0x30..0xE0)
        assertTrue(mixedRed != 0xFF)
        assertTrue(mixedRed != 0x22)
    }
}
