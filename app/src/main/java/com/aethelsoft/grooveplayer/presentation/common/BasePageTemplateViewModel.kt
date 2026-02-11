package com.aethelsoft.grooveplayer.presentation.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    protected val _uiState = MutableStateFlow<UiState<Any>>(UiState.Idle)
    val uiState: StateFlow<UiState<Any>> = _uiState.asStateFlow()

    open fun setLoading() {
        _uiState.value = UiState.Loading(null)
    }

    open fun setError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    open fun <T> setSuccess(data: T) {
        _uiState.value = UiState.Success(data as Any)
    }

    open fun refresh(){}
}