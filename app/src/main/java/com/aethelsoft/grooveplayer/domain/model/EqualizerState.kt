package com.aethelsoft.grooveplayer.domain.model

/**
 * Represents the state of the equalizer
 */
data class EqualizerState(
    val isEnabled: Boolean = false,
    val isAvailable: Boolean = false,
    val numberOfBands: Int = 0,
    val bandLevels: List<Int> = emptyList(), // Levels in millibels (-1500 to 1500)
    val bandFrequencies: List<Int> = emptyList(), // Center frequencies in Hz
    val currentPreset: Int = -1, // -1 means custom
    val availablePresets: List<String> = emptyList(),
    val levelRange: Pair<Int, Int> = Pair(-1500, 1500) // Min and max level in millibels
)
