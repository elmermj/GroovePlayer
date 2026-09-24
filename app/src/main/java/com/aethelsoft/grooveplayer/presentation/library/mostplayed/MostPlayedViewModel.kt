package com.aethelsoft.grooveplayer.presentation.library.mostplayed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.MostPlayedTrack
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetMostPlayedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MostPlayedViewModel @Inject constructor(
    getMostPlayedUseCase: GetMostPlayedUseCase,
) : ViewModel() {

    val mostPlayed: StateFlow<List<MostPlayedTrack>> = getMostPlayedUseCase(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
