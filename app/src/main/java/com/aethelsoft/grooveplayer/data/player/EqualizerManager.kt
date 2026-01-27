package com.aethelsoft.grooveplayer.data.player

import android.media.audiofx.Equalizer
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android Equalizer AudioEffect for real-time audio processing.
 * Handles initialization, band control, and preset management.
 */
@Singleton
class EqualizerManager @Inject constructor() {
    
    companion object {
        private const val TAG = "EqualizerManager"
        private const val MIN_LEVEL = -1500 // -15.0 dB in millibels
        private const val MAX_LEVEL = 1500  // +15.0 dB in millibels
    }
    
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = 0
    private var isEnabled: Boolean = false
    
    /**
     * Initialize equalizer with audio session ID from ExoPlayer
     */
    fun initialize(audioSessionId: Int): Boolean {
        if (audioSessionId == 0) {
            Log.w(TAG, "Cannot initialize equalizer: audio session ID is 0")
            return false
        }
        
        return try {
            release()
            equalizer = Equalizer(Int.MAX_VALUE, audioSessionId)
            this.audioSessionId = audioSessionId
            isEnabled = equalizer?.enabled == true
            Log.d(TAG, "Equalizer initialized with session ID: $audioSessionId, enabled: $isEnabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize equalizer: ${e.message}", e)
            equalizer = null
            false
        }
    }
    
    /**
     * Release equalizer resources
     */
    fun release() {
        try {
            equalizer?.release()
            equalizer = null
            audioSessionId = 0
            isEnabled = false
            Log.d(TAG, "Equalizer released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing equalizer: ${e.message}", e)
        }
    }
    
    /**
     * Check if equalizer is available and initialized
     */
    fun isAvailable(): Boolean = equalizer != null
    
    /**
     * Get number of frequency bands
     */
    fun getNumberOfBands(): Int {
        return try {
            equalizer?.numberOfBands?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting number of bands: ${e.message}", e)
            0
        }
    }
    
    /**
     * Get center frequency for a band in Hz
     */
    fun getCenterFrequency(band: Int): Int {
        return try {
            equalizer?.getCenterFreq(band.toShort())?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting center frequency for band $band: ${e.message}", e)
            0
        }
    }
    
    /**
     * Get frequency range for a band [min, max] in Hz
     */
    fun getBandFrequencyRange(band: Int): Pair<Int, Int> {
        return try {
            val range = equalizer?.getBandFreqRange(band.toShort())
            Pair(range?.get(0)?.toInt() ?: 0, range?.get(1)?.toInt() ?: 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting frequency range for band $band: ${e.message}", e)
            Pair(0, 0)
        }
    }
    
    /**
     * Get current level for a band in millibels
     */
    fun getBandLevel(band: Int): Short {
        return try {
            equalizer?.getBandLevel(band.toShort()) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting band level for band $band: ${e.message}", e)
            0
        }
    }
    
    /**
     * Set level for a band in millibels (-1500 to 1500)
     */
    fun setBandLevel(band: Int, level: Int) {
        try {
            val clampedLevel = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
            equalizer?.setBandLevel(band.toShort(), clampedLevel.toShort())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level for band $band: ${e.message}", e)
        }
    }
    
    /**
     * Get minimum and maximum level range in millibels
     */
    fun getLevelRange(): Pair<Int, Int> {
        return try {
            val range = equalizer?.bandLevelRange
            if (range != null && range.size >= 2) {
                Pair(range[0].toInt(), range[1].toInt())
            } else {
                Pair(MIN_LEVEL, MAX_LEVEL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting level range: ${e.message}", e)
            Pair(MIN_LEVEL, MAX_LEVEL)
        }
    }
    
    /**
     * Get number of presets
     */
    fun getNumberOfPresets(): Int {
        return try {
            equalizer?.numberOfPresets?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting number of presets: ${e.message}", e)
            0
        }
    }
    
    /**
     * Get preset name by index
     */
    fun getPresetName(preset: Int): String {
        return try {
            equalizer?.getPresetName(preset.toShort()) ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preset name for preset $preset: ${e.message}", e)
            "Unknown"
        }
    }
    
    /**
     * Get all preset names
     */
    fun getAllPresetNames(): List<String> {
        val presets = mutableListOf<String>()
        val count = getNumberOfPresets()
        for (i in 0 until count) {
            presets.add(getPresetName(i))
        }
        return presets
    }
    
    /**
     * Set preset by index
     */
    fun setPreset(preset: Int) {
        try {
            equalizer?.usePreset(preset.toShort())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting preset $preset: ${e.message}", e)
        }
    }
    
    /**
     * Get current preset index (-1 if custom)
     */
    fun getCurrentPreset(): Int {
        return try {
            equalizer?.currentPreset?.toInt() ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current preset: ${e.message}", e)
            -1
        }
    }
    
    /**
     * Enable or disable equalizer
     */
    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            isEnabled = enabled
            Log.d(TAG, "Equalizer ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting equalizer enabled state: ${e.message}", e)
        }
    }
    
    /**
     * Check if equalizer is enabled
     */
    fun getEnabled(): Boolean = isEnabled && equalizer?.enabled == true
    
    /**
     * Get all band levels as a list
     */
    fun getAllBandLevels(): List<Int> {
        return try {
            val levels = mutableListOf<Int>()
            val numBands = getNumberOfBands()
            for (i in 0 until numBands) {
                levels.add(getBandLevel(i).toInt())
            }
            levels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all band levels: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Set all band levels from a list
     */
    fun setAllBandLevels(levels: List<Int>) {
        levels.forEachIndexed { index, level ->
            setBandLevel(index, level)
        }
    }
    
    /**
     * Reset all bands to 0 (flat)
     */
    fun reset() {
        val numBands = getNumberOfBands()
        for (i in 0 until numBands) {
            setBandLevel(i, 0)
        }
        Log.d(TAG, "Equalizer reset to flat")
    }
}
