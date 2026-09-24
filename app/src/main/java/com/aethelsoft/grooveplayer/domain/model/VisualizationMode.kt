package com.aethelsoft.grooveplayer.domain.model

/**
 * Visualization mode for the audio-reactive glow.
 *
 * - OFF: no visualization (glow uses neutral/default values)
 * - SIMULATED: time-based template animation. Not live audio analysis.
 *   User-facing label: "Simulated".
 * - REAL_TIME: Visualizer FFT (requires RECORD_AUDIO). User-facing label: "Dynamic".
 */
enum class VisualizationMode {
    OFF,
    SIMULATED,
    REAL_TIME
}

