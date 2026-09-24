package com.aethelsoft.grooveplayer.domain.repository

import android.app.Activity
import com.aethelsoft.grooveplayer.domain.model.BillingProduct
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    fun observeProducts(): Flow<List<BillingProduct>>
    fun observeIsReady(): Flow<Boolean>
    fun observePurchaseInFlight(): Flow<Boolean>
    fun observeError(): Flow<String?>

    /** Connect BillingClient and query product details. Safe to call repeatedly. */
    suspend fun ensureReady()

    suspend fun launchPurchase(activity: Activity, productId: String): Result<Unit>

    /** Re-query owned purchases, acknowledge, and refresh entitlements via /v1/me when possible. */
    suspend fun restorePurchases(): Result<Unit>

    /**
     * POST /v1/billing/addons/cancel — stop renewal only (`cancel_at_period_end`).
     * Keeps current quota until period end; does not start trim/over_quota.
     */
    suspend fun cancelStorageAddon(addonId: String): Result<Unit>

    fun clearError()
}
