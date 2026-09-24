package com.aethelsoft.grooveplayer.presentation.ads

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.usecase.ads_category.ShouldShowAdsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.ads_category.StartupAdQuotaUseCase
import com.aethelsoft.grooveplayer.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    application: Application,
    shouldShowAdsUseCase: ShouldShowAdsUseCase,
    private val startupAdQuotaUseCase: StartupAdQuotaUseCase,
) : BaseViewModel(application) {

    val shouldShowAds: StateFlow<Boolean> = shouldShowAdsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun canShowStartupAd(): Boolean = shouldShowAds.value && startupAdQuotaUseCase.canShow()

    fun recordStartupAdShown() = startupAdQuotaUseCase.recordShown()

    override fun refresh() {
        setSuccess(Unit)
    }
}
