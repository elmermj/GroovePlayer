package com.aethelsoft.grooveplayer.data.repository

import android.util.Log
import com.aethelsoft.grooveplayer.data.player.EqualizerManager
import com.aethelsoft.grooveplayer.domain.model.EqualizerState
import com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerRepositoryImpl @Inject constructor(
    private val equalizerManager: EqualizerManager,
    private val userRepository: com.aethelsoft.grooveplayer.domain.repository.UserRepository
) : EqualizerRepository {
    
    private val _equalizerState = MutableStateFlow(createEqualizerState())
    
    init {
        // Don't update state in init - wait for equalizer to be initialized
        // State will be updated when equalizer is actually initialized
    }
    
    private fun createEqualizerState(): EqualizerState {
        return try {
            if (!equalizerManager.isAvailable()) {
                return EqualizerState()
            }
            
            val numBands = equalizerManager.getNumberOfBands()
            val bandLevels = if (numBands > 0) {
                try {
                    equalizerManager.getAllBandLevels()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            val bandFrequencies = if (numBands > 0) {
                try {
                    (0 until numBands).map { equalizerManager.getCenterFrequency(it) }
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            EqualizerState(
                isEnabled = try { equalizerManager.getEnabled() } catch (e: Exception) { false },
                isAvailable = equalizerManager.isAvailable(),
                numberOfBands = numBands,
                bandLevels = bandLevels,
                bandFrequencies = bandFrequencies,
                currentPreset = try { equalizerManager.getCurrentPreset() } catch (e: Exception) { -1 },
                availablePresets = try { equalizerManager.getAllPresetNames() } catch (e: Exception) { emptyList() },
                levelRange = try { equalizerManager.getLevelRange() } catch (e: Exception) { Pair(-1500, 1500) }
            )
        } catch (e: Exception) {
            android.util.Log.e("EqualizerRepositoryImpl", "Error creating equalizer state: ${e.message}", e)
            EqualizerState()
        }
    }
    
    private fun updateState() {
        try {
            _equalizerState.value = createEqualizerState()
        } catch (e: Exception) {
            Log.e("EqualizerRepositoryImpl", "Error updating state: ${e.message}", e)
            _equalizerState.value = EqualizerState()
        }
    }
    
    /**
     * Refresh the equalizer state (called when equalizer is initialized)
     */
    fun refreshState() {
        updateState()
    }
    
    override fun observeEqualizerState(): Flow<EqualizerState> = _equalizerState.asStateFlow()
    
    override suspend fun setBandLevel(band: Int, level: Int) {
        equalizerManager.setBandLevel(band, level)
        updateState()
        // Real-time changes - don't persist automatically
    }
    
    override suspend fun setAllBandLevels(levels: List<Int>) {
        equalizerManager.setAllBandLevels(levels)
        updateState()
        // Real-time changes - don't persist automatically
    }
    
    override suspend fun setPreset(preset: Int) {
        equalizerManager.setPreset(preset)
        updateState()
        // Real-time changes - don't persist automatically
    }
    
    override suspend fun setEnabled(enabled: Boolean) {
        equalizerManager.setEnabled(enabled)
        updateState()
        // Real-time changes - don't persist automatically
    }
    
    override suspend fun reset() {
        equalizerManager.reset()
        updateState()
        // Real-time changes - don't persist automatically
    }
    
    /**
     * Load equalizer settings from user settings
     */
    override suspend fun loadSettings() {
        try {
            if (!equalizerManager.isAvailable()) {
                Log.d("EqualizerRepositoryImpl", "Equalizer not available, skipping load")
                return
            }
            
            val settings = try {
                userRepository.getUserSettings()
            } catch (e: Exception) {
                Log.e("EqualizerRepositoryImpl", "Error getting user settings: ${e.message}", e)
                return
            }
            
            val numBands = try {
                equalizerManager.getNumberOfBands()
            } catch (e: Exception) {
                Log.e("EqualizerRepositoryImpl", "Error getting number of bands: ${e.message}", e)
                return
            }
            
            // Restore enabled state first
            try {
                equalizerManager.setEnabled(settings.equalizerEnabled)
            } catch (e: Exception) {
                Log.e("EqualizerRepositoryImpl", "Error setting enabled state: ${e.message}", e)
            }
            
            // Restore preset if not custom and preset is valid
            try {
                if (settings.equalizerPreset >= 0) {
                    val numPresets = equalizerManager.getNumberOfPresets()
                    if (settings.equalizerPreset < numPresets) {
                        equalizerManager.setPreset(settings.equalizerPreset)
                    } else if (settings.equalizerBandLevels.isNotEmpty() && 
                               settings.equalizerBandLevels.size == numBands) {
                        // If preset is invalid or custom, restore band levels
                        equalizerManager.setAllBandLevels(settings.equalizerBandLevels)
                    }
                } else if (settings.equalizerBandLevels.isNotEmpty() && 
                           settings.equalizerBandLevels.size == numBands) {
                    // Custom preset - restore band levels
                    equalizerManager.setAllBandLevels(settings.equalizerBandLevels)
                }
            } catch (e: Exception) {
                Log.e("EqualizerRepositoryImpl", "Error restoring preset/bands: ${e.message}", e)
            }
            
            updateState()
            Log.d("EqualizerRepositoryImpl", "Loaded equalizer settings: enabled=${settings.equalizerEnabled}, preset=${settings.equalizerPreset}, bands=${settings.equalizerBandLevels.size}")
        } catch (e: Exception) {
            Log.e("EqualizerRepositoryImpl", "Error loading equalizer settings: ${e.message}", e)
        }
    }
    
    /**
     * Persist current equalizer settings to user settings
     * Called explicitly when user clicks "Save Settings"
     */
    override suspend fun saveSettings() {
        try {
            val state = _equalizerState.value
            userRepository.updateEqualizerSettings(
                enabled = state.isEnabled,
                preset = state.currentPreset,
                bandLevels = state.bandLevels
            )
            updateState() // Refresh state after saving
            Log.d("EqualizerRepositoryImpl", "Saved equalizer settings")
        } catch (e: Exception) {
            Log.e("EqualizerRepositoryImpl", "Error saving equalizer settings: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Gets the saved equalizer settings from user settings
     */
    override suspend fun getSavedSettings(): EqualizerState? {
        return try {
            val settings = userRepository.getUserSettings()
            if (settings.equalizerBandLevels.isEmpty()) {
                null
            } else {
                EqualizerState(
                    isEnabled = settings.equalizerEnabled,
                    isAvailable = equalizerManager.isAvailable(),
                    numberOfBands = equalizerManager.getNumberOfBands(),
                    bandLevels = settings.equalizerBandLevels,
                    bandFrequencies = if (settings.equalizerBandLevels.isNotEmpty()) {
                        try {
                            (0 until settings.equalizerBandLevels.size).map { 
                                equalizerManager.getCenterFrequency(it) 
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    },
                    currentPreset = settings.equalizerPreset,
                    availablePresets = try { equalizerManager.getAllPresetNames() } catch (e: Exception) { emptyList() },
                    levelRange = try { equalizerManager.getLevelRange() } catch (e: Exception) { Pair(-1500, 1500) }
                )
            }
        } catch (e: Exception) {
            Log.e("EqualizerRepositoryImpl", "Error getting saved settings: ${e.message}", e)
            null
        }
    }
}
