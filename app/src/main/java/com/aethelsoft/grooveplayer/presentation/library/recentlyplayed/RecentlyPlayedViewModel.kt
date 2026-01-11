package com.aethelsoft.grooveplayer.presentation.library.recentlyplayed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.usecase.home_category.GetRecentlyPlayedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    application: Application,
    private val getRecentlyPlayedUseCase: GetRecentlyPlayedUseCase
) : AndroidViewModel(application) {
    
    val recentlyPlayed: StateFlow<List<Song>> = getRecentlyPlayedUseCase(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

