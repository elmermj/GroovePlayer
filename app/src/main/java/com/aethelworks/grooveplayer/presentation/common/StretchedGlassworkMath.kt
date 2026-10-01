package com.aethelworks.grooveplayer.presentation.common

/**
 * Crop and blur math for [StretchedGlasswork]. Pure so list rows can cache a
 * result instead of sampling pixels while scrolling.
 */
internal data class SquareCrop(
    val left: Int,
    val top: Int,
    val size: Int,
) {
    val edgeX: Int get() = left + size - 1
}

internal fun centerSquareCrop(width: Int, height: Int): SquareCrop? {
    if (width <= 0 || height <= 0) return null
    val size = minOf(width, height)
    val left = (width - size) / 2
    val top = (height - size) / 2
    return SquareCrop(left = left, top = top, size = size)
}

internal fun stretchedGlassworkModelKey(model: Any?): String? = when (model) {
    null -> null
    is String -> model.trim().takeIf { it.isNotEmpty() }
    else -> "model:${model.javaClass.name}:${model.hashCode()}"
}

/** Average relative luminance in 0..1. */
internal fun averageLuminance(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0f
    var sum = 0.0
    for (color in pixels) {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        sum += (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0
    }
    return (sum / pixels.size).toFloat()
}

/**
 * Dark veil over the stretched color. Pale artwork gets a heavier veil so
 * white title text stays readable; saturated artwork keeps more of its color.
 * Unblurred rows (no RenderEffect) start darker.
 */
internal fun scrimAlphaFor(luminance: Float, blurred: Boolean): Float {
    val base = if (blurred) 0.36f else 0.62f
    val lift = (luminance - 0.4f).coerceAtLeast(0f) * if (blurred) 0.45f else 0.2f
    return (base + lift).coerceIn(0.3f, 0.78f)
}

/** Separable box blur. Returns a new buffer. Radius 0 copies [pixels]. */
internal fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
    if (width <= 0 || height <= 0 || pixels.size < width * height) return pixels
    if (radius <= 0) return pixels.copyOf()
    val horizontal = IntArray(width * height)
    val vertical = IntArray(width * height)
    val window = radius * 2 + 1
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val sampleX = (x + offset).coerceIn(0, width - 1)
                val color = pixels[row + sampleX]
                alpha += (color ushr 24) and 0xFF
                red += (color ushr 16) and 0xFF
                green += (color ushr 8) and 0xFF
                blue += color and 0xFF
            }
            horizontal[row + x] = argb(alpha / window, red / window, green / window, blue / window)
        }
    }
    for (x in 0 until width) {
        for (y in 0 until height) {
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val sampleY = (y + offset).coerceIn(0, height - 1)
                val color = horizontal[sampleY * width + x]
                alpha += (color ushr 24) and 0xFF
                red += (color ushr 16) and 0xFF
                green += (color ushr 8) and 0xFF
                blue += color and 0xFF
            }
            vertical[y * width + x] = argb(alpha / window, red / window, green / window, blue / window)
        }
    }
    return vertical
}

private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
