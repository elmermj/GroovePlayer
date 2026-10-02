package com.aethelworks.grooveplayer.presentation.profile

import androidx.lifecycle.ViewModel
import com.aethelworks.grooveplayer.domain.model.RecentUpdates
import com.aethelworks.grooveplayer.domain.usecase.profile_category.GetRecentUpdatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecentUpdatesViewModel @Inject constructor(
    getRecentUpdates: GetRecentUpdatesUseCase,
) : ViewModel() {
    val updates: RecentUpdates = getRecentUpdates()
}
