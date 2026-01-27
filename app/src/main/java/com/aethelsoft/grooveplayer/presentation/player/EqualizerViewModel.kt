package com.aethelsoft.grooveplayer.presentation.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.EqualizerState
import com.aethelsoft.grooveplayer.domain.usecase.equalizer_category.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing equalizer state.
 * Follows MVVM and Clean Architecture by only using UseCases.
 */
@HiltViewModel
class EqualizerViewModel @Inject constructor(
    application: Application,
    private val observeEqualizerStateUseCase: ObserveEqualizerStateUseCase,
    private val setEqualizerBandLevelUseCase: SetEqualizerBandLevelUseCase,
    private val setEqualizerPresetUseCase: SetEqualizerPresetUseCase,
    private val setEqualizerEnabledUseCase: SetEqualizerEnabledUseCase,
    private val resetEqualizerUseCase: ResetEqualizerUseCase,
    private val saveEqualizerSettingsUseCase: SaveEqualizerSettingsUseCase,
    private val equalizerRepository: com.aethelsoft.grooveplayer.domain.repository.EqualizerRepository
) : AndroidViewModel(application) {

    val equalizerState: StateFlow<EqualizerState> = 
        observeEqualizerStateUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, EqualizerState())
    
    private val _savedState = MutableStateFlow<EqualizerState?>(null)
    
    val hasUnsavedChanges: StateFlow<Boolean> = combine(
        equalizerState,
        _savedState
    ) { current, saved ->
        if (saved == null) {
            // No saved state yet, check if current state differs from defaults
            current.isEnabled != false || 
            current.currentPreset != -1 || 
            current.bandLevels.any { it != 0 }
        } else {
            // Compare current state with saved state
            current.isEnabled != saved.isEnabled ||
            current.currentPreset != saved.currentPreset ||
            current.bandLevels != saved.bandLevels
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    init {
        // Load saved state on initialization
        viewModelScope.launch {
            _savedState.value = equalizerRepository.getSavedSettings()
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        viewModelScope.launch {
            setEqualizerBandLevelUseCase(band, level)
        }
    }

    fun setPreset(preset: Int) {
        viewModelScope.launch {
            setEqualizerPresetUseCase(preset)
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setEqualizerEnabledUseCase(enabled)
        }
    }

    fun reset() {
        viewModelScope.launch {
            resetEqualizerUseCase()
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            saveEqualizerSettingsUseCase()
            // Update saved state after saving
            _savedState.value = equalizerRepository.getSavedSettings()
        }
    }
}
