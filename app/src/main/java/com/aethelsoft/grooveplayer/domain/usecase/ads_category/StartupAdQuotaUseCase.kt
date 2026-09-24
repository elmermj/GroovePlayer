package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import com.aethelsoft.grooveplayer.domain.repository.StartupAdQuotaRepository
import javax.inject.Inject

/** Startup video/interstitial: max 2 impressions per calendar day (local). */
class StartupAdQuotaUseCase @Inject constructor(
    private val quotaRepository: StartupAdQuotaRepository,
) {
    fun canShow(): Boolean = quotaRepository.canShow()
    fun recordShown() = quotaRepository.recordShown()
}
