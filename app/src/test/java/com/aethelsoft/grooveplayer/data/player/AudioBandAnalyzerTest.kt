package com.aethelsoft.grooveplayer.data.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioBandAnalyzerTest {

    @Test
    fun sampleRate_usesMilliHertzWhenProvided() {
        assertEquals(44_100f, visualizerSampleRateHz(44_100_000), 0.01f)
        assertEquals(48_000f, visualizerSampleRateHz(48_000_000), 0.01f)
        assertEquals(44_100f, visualizerSampleRateHz(0), 0.01f)
        assertEquals(44_100f, visualizerSampleRateHz(-5), 0.01f)
    }

    @Test
    fun bands_mapSubBassPresenceAndTrebleOntoUiFields() {
        val analyzer = AudioBandAnalyzer()
        val binHz = 100f

        val bassOnly = analyzer.analyze(fillBand(binHz, 60f, 250f), binHz, nowMs = 0L)
        assertEquals(AudioBandAnalyzer.ATTACK_BASS, bassOnly.bass, 0.001f)
        assertEquals(0f, bassOnly.mid, 0.001f)
        assertEquals(0f, bassOnly.treble, 0.001f)

        val presence = AudioBandAnalyzer().analyze(fillBand(binHz, 1000f, 3000f), binHz, nowMs = 0L)
        assertEquals(0f, presence.bass, 0.001f)
        assertEquals(0.6f * AudioBandAnalyzer.ATTACK_PRESENCE, presence.mid, 0.001f)
        assertEquals(0f, presence.treble, 0.001f)

        val highMid = AudioBandAnalyzer().analyze(fillBand(binHz, 3000f, 6000f), binHz, nowMs = 0L)
        assertEquals(0.35f * AudioBandAnalyzer.ATTACK_HIGH_MID, highMid.treble, 0.001f)

        val treble = AudioBandAnalyzer().analyze(fillBand(binHz, 6000f, 16000f), binHz, nowMs = 0L)
        assertEquals(0.65f * AudioBandAnalyzer.ATTACK_TREBLE, treble.treble, 0.001f)

        val aboveRange = AudioBandAnalyzer().analyze(fillBand(binHz, 16000f, 22000f), binHz, nowMs = 0L)
        assertEquals(0f, aboveRange.bass, 0.001f)
        assertEquals(0f, aboveRange.mid, 0.001f)
        assertEquals(0f, aboveRange.treble, 0.001f)
    }

    @Test
    fun bassUi_usesTheLouderOfSubAndBass() {
        val binHz = 20f
        val sub = AudioBandAnalyzer().analyze(fillBand(binHz, 20f, 60f), binHz, nowMs = 0L)
        assertEquals(AudioBandAnalyzer.ATTACK_SUB, sub.bass, 0.001f)
    }

    @Test
    fun envelope_attacksFasterThanItReleases() {
        val analyzer = AudioBandAnalyzer()
        val binHz = 100f
        val hit = analyzer.analyze(fillBand(binHz, 60f, 250f), binHz, nowMs = 0L)
        val release = analyzer.analyze(FloatArray(0), binHz, nowMs = 30L)
        val rise = hit.bass
        val fall = hit.bass - release.bass
        assertTrue(rise > fall)
        assertEquals(hit.bass * (1f - AudioBandAnalyzer.RELEASE_BASS), release.bass, 0.001f)
    }

    @Test
    fun beat_requiresDeltaAndRefractory_andDecaysWithoutPowerCurve() {
        val analyzer = AudioBandAnalyzer()
        val binHz = 100f
        val loud = fillBand(binHz, 60f, 250f)

        val onset = analyzer.analyze(loud, binHz, nowMs = 1_000L)
        assertEquals(1f, onset.beat, 0.0001f)

        val blocked = analyzer.analyze(loud, binHz, nowMs = 1_100L)
        assertEquals(1f * (1f - AudioBandAnalyzer.BEAT_RELEASE), blocked.beat, 0.0001f)

        repeat(12) { index ->
            analyzer.analyze(FloatArray(0), binHz, nowMs = 1_200L + index * 20L)
        }
        val stillRefractory = analyzer.analyze(loud, binHz, nowMs = 1_250L)
        assertTrue(stillRefractory.beat < 1f)

        repeat(20) {
            analyzer.analyze(FloatArray(0), binHz, nowMs = 2_000L)
        }
        val again = analyzer.analyze(loud, binHz, nowMs = 2_000L)
        assertEquals(1f, again.beat, 0.0001f)
    }

    private fun fillBand(binHz: Float, lowHz: Float, highHz: Float): FloatArray {
        val size = (22_000f / binHz).toInt() + 1
        return FloatArray(size).also { magnitudes ->
            for (i in magnitudes.indices) {
                val freq = i * binHz
                if (freq >= lowHz && freq < highHz) magnitudes[i] = 128f
            }
        }
    }
}
