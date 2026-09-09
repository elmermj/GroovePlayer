package com.aethelsoft.grooveplayer.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyle
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCodec
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleIds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Observes persisted UI style for [com.aethelsoft.grooveplayer.utils.theme.ui.GroovePlayerTheme].
 * The styling screen saves via [UserRepository.updateUiStyle]; this VM picks it up automatically.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    val style: StateFlow<GrooveStyle> = userRepository.observeUserSettings()
        .map { settings ->
            GrooveStyleCatalog.resolve(settings.uiStyleId, settings.uiStyleOverrides)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GrooveStyleCatalog.Default,
        )

    fun setStylePreset(styleId: String) {
        viewModelScope.launch {
            userRepository.updateUiStyle(styleId, "")
        }
    }

    fun saveCustomStyle(style: GrooveStyle) {
        viewModelScope.launch {
            val custom = style.copy(id = GrooveStyleIds.CUSTOM, displayName = "Custom")
            userRepository.updateUiStyle(
                styleId = GrooveStyleIds.CUSTOM,
                overrides = GrooveStyleCodec.encode(custom),
            )
        }
    }
}
