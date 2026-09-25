package com.aethelsoft.grooveplayer.presentation.ads

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.usecase.ads_category.ShouldShowAdsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.ads_category.StartupAdQuotaUseCase
import com.aethelsoft.grooveplayer.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    application: Application,
    private val shouldShowAdsUseCase: ShouldShowAdsUseCase,
    private val startupAdQuotaUseCase: StartupAdQuotaUseCase,
) : BaseViewModel(application) {

    /** Closed until entitlement is confirmed Free. Never defaults to showing ads. */
    val shouldShowAds: StateFlow<Boolean> = shouldShowAdsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun canShowStartupAd(): Boolean =
        shouldShowAdsUseCase.canShowNow() && startupAdQuotaUseCase.canShow()

    /**
     * Suspends until session restore confirms a tier. Then false for Basic/Premium
     * and for an exhausted daily quota; true only for confirmed Free with quota left.
     */
    suspend fun awaitCanShowStartupAd(): Boolean {
        shouldShowAdsUseCase.observeDecision().first { it != null }
        return canShowStartupAd()
    }

    fun recordStartupAdShown() = startupAdQuotaUseCase.recordShown()

    override fun refresh() {
        setSuccess(Unit)
    }
}
