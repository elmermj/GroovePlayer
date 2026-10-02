package com.aethelworks.grooveplayer.domain.usecase.billing_category

import com.aethelworks.grooveplayer.domain.repository.BillingRepository
import javax.inject.Inject

class EnsureBillingReadyUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke() = billingRepository.ensureReady()
}
