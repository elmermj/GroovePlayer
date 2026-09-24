package com.aethelsoft.grooveplayer.data.player

import kotlin.math.log10
import kotlin.math.max

/**
 * Live frequency-band analysis for the artwork glow.
 *
 * Six bands are enveloped with a fast attack and a slow release, then folded
 * into the bass / mid / treble fields the UI already reads. Beat onsets are
 * sparse: a bass jump has to clear both a local-mean ratio and a minimum
 * delta, and a second onset is ignored for [BEAT_REFRACTORY_MS].
 *
 * This is the Dynamic (Visualizer) path. The SIMULATED mode elsewhere is a
 * time-based template, not this analysis.
 */
internal class AudioBandAnalyzer {

    private var subEnv = 0f
    private var bassEnv = 0f
    private var lowMidEnv = 0f
    private var presenceEnv = 0f
    private var highMidEnv = 0f
    private var trebleEnv = 0f
    private var previousBass = 0f
    private var beatLevel = 0f
    private var lastOnsetMs: Long? = null
    private val bassHistory = ArrayDeque<Float>()

    fun reset() {
        synchronized(this) {
            subEnv = 0f
            bassEnv = 0f
            lowMidEnv = 0f
            presenceEnv = 0f
            highMidEnv = 0f
            trebleEnv = 0f
            previousBass = 0f
            beatLevel = 0f
            lastOnsetMs = null
            bassHistory.clear()
        }
    }

    fun analyze(magnitudes: FloatArray, binHz: Float, nowMs: Long): AnalyzedBands {
        synchronized(this) {
            val raw = rawBands(magnitudes, binHz)
            subEnv = envelope(subEnv, raw.sub, ATTACK_SUB, RELEASE_SUB)
            bassEnv = envelope(bassEnv, raw.bass, ATTACK_BASS, RELEASE_BASS)
            lowMidEnv = envelope(lowMidEnv, raw.lowMid, ATTACK_LOW_MID, RELEASE_LOW_MID)
            presenceEnv = envelope(presenceEnv, raw.presence, ATTACK_PRESENCE, RELEASE_PRESENCE)
            highMidEnv = envelope(highMidEnv, raw.highMid, ATTACK_HIGH_MID, RELEASE_HIGH_MID)
            trebleEnv = envelope(trebleEnv, raw.treble, ATTACK_TREBLE, RELEASE_TREBLE)

            val bass = max(subEnv, bassEnv)
            val mid = 0.4f * lowMidEnv + 0.6f * presenceEnv
            val treble = 0.35f * highMidEnv + 0.65f * trebleEnv

            val localMean = meanOf(bassHistory)
            val delta = bass - previousBass
            val sinceOnset = lastOnsetMs?.let { nowMs - it } ?: Long.MAX_VALUE
            val onset = bass > localMean * BEAT_MEAN_RATIO &&
                delta > BEAT_MIN_DELTA &&
                sinceOnset >= BEAT_REFRACTORY_MS
            if (onset) {
                lastOnsetMs = nowMs
                beatLevel = 1f
            } else {
                beatLevel *= 1f - BEAT_RELEASE
                if (beatLevel < 0.001f) beatLevel = 0f
            }
            previousBass = bass
            bassHistory.addLast(bass)
            while (bassHistory.size > MEAN_WINDOW) {
                bassHistory.removeFirst()
            }

            return AnalyzedBands(
                bass = bass.coerceIn(0f, 1f),
                mid = mid.coerceIn(0f, 1f),
                treble = treble.coerceIn(0f, 1f),
                beat = beatLevel.coerceIn(0f, 1f),
            )
        }
    }

    private fun rawBands(magnitudes: FloatArray, binHz: Float): RawBands {
        if (binHz <= 0f || magnitudes.isEmpty()) return RawBands()
        var subSum = 0.0
        var subCount = 0
        var bassSum = 0.0
        var bassCount = 0
        var lowMidSum = 0.0
        var lowMidCount = 0
        var presenceSum = 0.0
        var presenceCount = 0
        var highMidSum = 0.0
        var highMidCount = 0
        var trebleSum = 0.0
        var trebleCount = 0

        for (i in magnitudes.indices) {
            val freq = i * binHz
            val magnitude = magnitudes[i].toDouble()
            when {
                freq >= 20f && freq < 60f -> {
                    subSum += magnitude
                    subCount++
                }
                freq >= 60f && freq < 250f -> {
                    bassSum += magnitude
                    bassCount++
                }
                freq >= 250f && freq < 1000f -> {
                    lowMidSum += magnitude
                    lowMidCount++
                }
                freq >= 1000f && freq < 3000f -> {
                    presenceSum += magnitude
                    presenceCount++
                }
                freq >= 3000f && freq < 6000f -> {
                    highMidSum += magnitude
                    highMidCount++
                }
                freq >= 6000f && freq < 16000f -> {
                    trebleSum += magnitude
                    trebleCount++
                }
            }
        }

        return RawBands(
            sub = perceptual(subSum, subCount),
            bass = perceptual(bassSum, bassCount),
            lowMid = perceptual(lowMidSum, lowMidCount),
            presence = perceptual(presenceSum, presenceCount),
            highMid = perceptual(highMidSum, highMidCount),
            treble = perceptual(trebleSum, trebleCount),
        )
    }

    private data class RawBands(
        val sub: Float = 0f,
        val bass: Float = 0f,
        val lowMid: Float = 0f,
        val presence: Float = 0f,
        val highMid: Float = 0f,
        val treble: Float = 0f,
    )

    companion object {
        const val ATTACK_SUB = 0.55f
        const val ATTACK_BASS = 0.62f
        const val ATTACK_LOW_MID = 0.58f
        const val ATTACK_PRESENCE = 0.60f
        const val ATTACK_HIGH_MID = 0.65f
        const val ATTACK_TREBLE = 0.70f

        const val RELEASE_SUB = 0.12f
        const val RELEASE_BASS = 0.15f
        const val RELEASE_LOW_MID = 0.16f
        const val RELEASE_PRESENCE = 0.18f
        const val RELEASE_HIGH_MID = 0.20f
        const val RELEASE_TREBLE = 0.22f

        const val BEAT_MEAN_RATIO = 1.45f
        const val BEAT_MIN_DELTA = 0.12f
        const val BEAT_REFRACTORY_MS = 300L
        const val BEAT_RELEASE = 0.18f
        const val MEAN_WINDOW = 20

        private fun envelope(current: Float, target: Float, attack: Float, release: Float): Float {
            val alpha = if (target > current) attack else release
            return (current * (1f - alpha) + target * alpha).coerceIn(0f, 1f)
        }

        private fun perceptual(sum: Double, count: Int): Float {
            if (count <= 0) return 0f
            val average = (sum / count) / 128.0
            val scaled = log10(1.0 + average.coerceAtLeast(0.0) * 9.0) / log10(10.0)
            return scaled.toFloat().coerceIn(0f, 1f)
        }

        private fun meanOf(values: ArrayDeque<Float>): Float {
            if (values.isEmpty()) return 0f
            var sum = 0f
            for (value in values) sum += value
            return sum / values.size
        }
    }
}

internal data class AnalyzedBands(
    val bass: Float,
    val mid: Float,
    val treble: Float,
    val beat: Float,
)

/**
 * Visualizer reports sampling rate in milliHertz. Fall back to 44.1 kHz when
 * the callback has not provided a rate yet.
 */
internal fun visualizerSampleRateHz(samplingRateMilliHz: Int): Float {
    return if (samplingRateMilliHz > 0) samplingRateMilliHz / 1000f else 44_100f
}
