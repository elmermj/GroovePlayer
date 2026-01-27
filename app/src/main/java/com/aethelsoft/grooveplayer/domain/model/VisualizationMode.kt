package com.aethelsoft.grooveplayer.domain.model

/**
 * Visualization mode for the audio-reactive glow / waveform.
 *
 * - OFF: no visualization (glow uses neutral/default values)
 * - SIMULATED: time-based template animation (no RECORD_AUDIO needed)
 * - REAL_TIME: real waveform/FFT data (requires RECORD_AUDIO + Visualizer)
 */
enum class VisualizationMode {
    OFF,
    SIMULATED,
    REAL_TIME
}

