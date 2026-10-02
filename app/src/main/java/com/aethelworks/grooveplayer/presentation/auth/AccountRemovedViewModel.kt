package com.aethelworks.grooveplayer.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelworks.grooveplayer.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccountRemovedViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val visible: StateFlow<Boolean> =
        authRepository.observeAccountRemoved()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun dismiss() {
        authRepository.acknowledgeAccountRemoved()
    }
}
