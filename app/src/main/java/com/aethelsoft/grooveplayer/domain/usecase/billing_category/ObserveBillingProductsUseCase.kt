package com.aethelsoft.grooveplayer.domain.usecase.billing_category

import com.aethelsoft.grooveplayer.domain.model.BillingProduct
import com.aethelsoft.grooveplayer.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBillingProductsUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    operator fun invoke(): Flow<List<BillingProduct>> = billingRepository.observeProducts()
}
