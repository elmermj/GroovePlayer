package com.aethelsoft.grooveplayer.presentation.common

/**
 * Sealed class representing UI states for data loading operations.
 * Used across all ViewModels for consistent state management.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    
    val isLoading: Boolean
        get() = this is Loading
    
    val isError: Boolean
        get() = this is Error
    
    val isSuccess: Boolean
        get() = this is Success
}

