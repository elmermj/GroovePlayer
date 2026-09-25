package com.aethelsoft.grooveplayer.domain.usecase.ads_category

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide latch. Starts closed so a fresh process (including the automatic
 * restart after library restore) cannot show ads before session restore confirms
 * the privilege tier.
 */
@Singleton
class AdEntitlementReadiness @Inject constructor() {
    private val resolved = MutableStateFlow(false)

    fun observe(): StateFlow<Boolean> = resolved.asStateFlow()

    fun isResolved(): Boolean = resolved.value

    fun markResolved() {
        resolved.value = true
    }
}
