package com.aethelworks.grooveplayer.presentation.equalizer

import org.junit.Assert.assertEquals
import org.junit.Test

class BandFrequencyFormatTest {
    @Test
    fun formatBandFreq_usesHzBelowOneKilohertzAndKhzAbove() {
        assertEquals("60 Hz", formatBandFreq(60))
        assertEquals("230 Hz", formatBandFreq(230))
        assertEquals("910 Hz", formatBandFreq(910))
        assertEquals("3.6 kHz", formatBandFreq(3_600))
        assertEquals("14 kHz", formatBandFreq(14_000))
    }

    @Test
    fun formatCenterFrequency_dividesMilliHertzFromTheEqualizerApi() {
        assertEquals("60 Hz", formatCenterFrequency(60_000))
        assertEquals("230 Hz", formatCenterFrequency(230_000))
        assertEquals("910 Hz", formatCenterFrequency(910_000))
        assertEquals("3.6 kHz", formatCenterFrequency(3_600_000))
        assertEquals("14 kHz", formatCenterFrequency(14_000_000))
    }
}
