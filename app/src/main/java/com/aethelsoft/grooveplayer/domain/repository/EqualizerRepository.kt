package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.EqualizerState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for equalizer operations.
 */
interface EqualizerRepository {
    /**
     * Observes the current equalizer state
     */
    fun observeEqualizerState(): Flow<EqualizerState>
    
    /**
     * Sets the level for a specific frequency band
     * @param band Index of the band (0 to numberOfBands-1)
     * @param level Level in millibels (-1500 to 1500)
     */
    suspend fun setBandLevel(band: Int, level: Int)
    
    /**
     * Sets all band levels at once
     */
    suspend fun setAllBandLevels(levels: List<Int>)
    
    /**
     * Sets a preset by index
     */
    suspend fun setPreset(preset: Int)
    
    /**
     * Enables or disables the equalizer
     */
    suspend fun setEnabled(enabled: Boolean)
    
    /**
     * Resets all bands to 0 (flat)
     */
    suspend fun reset()
    
    /**
     * Loads equalizer settings from user settings
     */
    suspend fun loadSettings()
    
    /**
     * Saves current equalizer settings to user settings
     */
    suspend fun saveSettings()
    
    /**
     * Gets the saved equalizer settings from user settings
     */
    suspend fun getSavedSettings(): EqualizerState?
}
