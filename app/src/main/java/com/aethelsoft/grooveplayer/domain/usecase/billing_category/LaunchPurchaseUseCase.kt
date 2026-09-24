package com.aethelsoft.grooveplayer.domain.usecase.billing_category

import android.app.Activity
import com.aethelsoft.grooveplayer.domain.repository.BillingRepository
import javax.inject.Inject

class LaunchPurchaseUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(activity: Activity, productId: String): Result<Unit> {
        billingRepository.ensureReady()
        return billingRepository.launchPurchase(activity, productId)
    }
}
