package com.aethelworks.grooveplayer.presentation.equalizer

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Label for an equalizer centre frequency already expressed in hertz.
 * 60, 230, 910, 3600 and 14000 become "60 Hz", "230 Hz", "910 Hz", "3.6 kHz" and "14 kHz".
 */
fun formatBandFreq(hz: Int): String {
    if (hz < 1000) return "$hz Hz"
    val khz = hz / 1000f
    val nearest = round(khz)
    val text = if (abs(khz - nearest) < 0.05f) {
        nearest.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", khz).removeSuffix(".0")
    }
    return "$text kHz"
}

/**
 * [android.media.audiofx.Equalizer.getCenterFreq] reports milliHertz.
 * Divide by 1000 before [formatBandFreq].
 */
fun formatCenterFrequency(milliHz: Int): String = formatBandFreq(milliHz / 1000)
