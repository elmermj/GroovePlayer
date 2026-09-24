package com.aethelsoft.grooveplayer.data.remote.api

import com.aethelsoft.grooveplayer.data.remote.dto.AckPurchaseRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.CancelAddonRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.CancelAddonResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.PublicUserDto
import com.aethelsoft.grooveplayer.data.remote.dto.VerifyPurchaseRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Play Billing ↔ backend — docs/play-billing.md + docs/addon-cancel-trim.md
 * - POST /v1/billing/play/verify → updated /v1/me-shaped user
 * - POST /v1/billing/play/ack → marks acknowledged server-side
 * - POST /v1/billing/addons/cancel → stop renewal only (cancel_at_period_end)
 */
interface BillingApi {
    @POST("/v1/billing/play/verify")
    suspend fun verifyPurchase(@Body body: VerifyPurchaseRequestDto): PublicUserDto

    @POST("/v1/billing/play/ack")
    suspend fun acknowledgePurchase(@Body body: AckPurchaseRequestDto): okhttp3.ResponseBody

    @POST("/v1/billing/addons/cancel")
    suspend fun cancelAddon(@Body body: CancelAddonRequestDto): CancelAddonResponseDto
}
