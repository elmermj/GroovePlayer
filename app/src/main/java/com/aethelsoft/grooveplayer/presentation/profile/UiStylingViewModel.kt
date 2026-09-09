package com.aethelsoft.grooveplayer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyle
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCatalog
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleCodec
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveStyleIds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UiStylingViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val committedStyle: StateFlow<GrooveStyle> = userRepository.observeUserSettings()
        .map { GrooveStyleCatalog.resolve(it.uiStyleId, it.uiStyleOverrides) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GrooveStyleCatalog.Default)

    private val _draft = MutableStateFlow(GrooveStyleCatalog.Default)
    val draft: StateFlow<GrooveStyle> = _draft.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait for the first DB emission — do not seed from stateIn's Default placeholder,
            // which previously marked the draft "initialized" before real settings arrived.
            val committed = userRepository.observeUserSettings()
                .map { GrooveStyleCatalog.resolve(it.uiStyleId, it.uiStyleOverrides) }
                .first()
            _draft.value = committed
            _isReady.value = true
        }
    }

    val isDirty: StateFlow<Boolean> = combine(draft, committedStyle, isReady) { d, c, ready ->
        ready && GrooveStyleCodec.encode(d) != GrooveStyleCodec.encode(c)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun applyPreset(style: GrooveStyle) {
        _draft.value = style
    }

    fun updateDraft(transform: (GrooveStyle) -> GrooveStyle) {
        _draft.update(transform)
    }

    fun discard() {
        _draft.value = committedStyle.value
    }

    /**
     * Persists the draft. Presets without edits save by id only;
     * any customized draft is stored as a full custom snapshot.
     */
    fun save(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val style = _draft.value
            val isUnmodifiedPreset = GrooveStyleCatalog.all.any {
                it.id == style.id && GrooveStyleCodec.encode(it) == GrooveStyleCodec.encode(style)
            }
            if (isUnmodifiedPreset) {
                userRepository.updateUiStyle(style.id, "")
            } else {
                val custom = style.copy(
                    id = GrooveStyleIds.CUSTOM,
                    displayName = "Custom",
                )
                userRepository.updateUiStyle(
                    styleId = GrooveStyleIds.CUSTOM,
                    overrides = GrooveStyleCodec.encode(custom),
                )
                _draft.value = custom
            }
            onSaved()
        }
    }
}
