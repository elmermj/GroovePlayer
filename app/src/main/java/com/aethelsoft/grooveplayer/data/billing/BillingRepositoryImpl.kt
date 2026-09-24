package com.aethelsoft.grooveplayer.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.aethelsoft.grooveplayer.data.mapper.AuthMapper
import com.aethelsoft.grooveplayer.data.remote.api.BillingApi
import com.aethelsoft.grooveplayer.data.remote.dto.AckPurchaseRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.CancelAddonRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.VerifyPurchaseRequestDto
import com.aethelsoft.grooveplayer.domain.model.BillingProduct
import com.aethelsoft.grooveplayer.domain.model.BillingProductKind
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BillingRepository
import com.aethelsoft.grooveplayer.utils.BillingProductIds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play Billing Library + Benny verify/ack contract.
 *
 * Flow per purchase:
 * 1. BillingClient purchase
 * 2. POST /v1/billing/play/verify → apply /v1/me-shaped user
 * 3. BillingClient.acknowledgePurchase (device)
 * 4. POST /v1/billing/play/ack (server)
 *
 * Dry-run backend: any non-empty token for known product_id is active (~1 month)
 * until GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is set.
 *
 * Max 5 storage packs — 6th → HTTP 409; surface user-facing error.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingApi: BillingApi,
    private val authRepository: AuthRepository,
) : BillingRepository, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    private val _products = MutableStateFlow<List<BillingProduct>>(emptyList())
    private val _ready = MutableStateFlow(false)
    private val _inFlight = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private var productDetailsById: Map<String, ProductDetails> = emptyMap()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    override fun observeProducts() = _products.asStateFlow()
    override fun observeIsReady() = _ready.asStateFlow()
    override fun observePurchaseInFlight() = _inFlight.asStateFlow()
    override fun observeError() = _error.asStateFlow()

    override fun clearError() {
        _error.value = null
    }

    override suspend fun ensureReady() {
        connectIfNeeded()
        queryProductDetailsInternal()
    }

    private suspend fun connectIfNeeded() {
        if (billingClient.isReady) {
            _ready.value = true
            return
        }
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                    _ready.value = ok
                    if (!ok) {
                        _error.value = "Billing unavailable (${result.debugMessage})"
                    }
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    _ready.value = false
                }
            })
        }
    }

    private suspend fun queryProductDetailsInternal() {
        if (!billingClient.isReady) return
        val productList = BillingProductIds.ALL.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
            // Keep catalog stubs so paywall still shows SKUs offline / before Play Console.
            _products.value = stubCatalog()
            return
        }
        val details = result.productDetailsList.orEmpty()
        productDetailsById = details.associateBy { it.productId }
        val mapped = details.mapNotNull { toDomain(it) }
        _products.value = if (mapped.isEmpty()) stubCatalog() else orderCatalog(mapped)
    }

    private fun stubCatalog(): List<BillingProduct> = listOf(
        BillingProduct(
            productId = BillingProductIds.BASIC_MONTHLY,
            title = "Basic",
            description = "No ads",
            formattedPrice = "$0.99",
            kind = BillingProductKind.BASIC_SUB,
        ),
        BillingProduct(
            productId = BillingProductIds.PREMIUM_MONTHLY,
            title = "Premium",
            description = "No ads + 40 GB cloud backup",
            formattedPrice = "Regional",
            kind = BillingProductKind.PREMIUM_SUB,
        ),
        BillingProduct(
            productId = BillingProductIds.STORAGE_20GB_MONTHLY,
            title = "+20 GB storage",
            description = "Stackable (max 5). Renews with Premium.",
            formattedPrice = "Regional",
            kind = BillingProductKind.STORAGE_ADDON,
        ),
    )

    private fun orderCatalog(list: List<BillingProduct>): List<BillingProduct> {
        val order = BillingProductIds.ALL
        return list.sortedBy { order.indexOf(it.productId).let { i -> if (i < 0) 99 else i } }
    }

    private fun toDomain(details: ProductDetails): BillingProduct? {
        val offer = details.subscriptionOfferDetails?.firstOrNull()
        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            ?: details.oneTimePurchaseOfferDetails?.formattedPrice
            ?: "—"
        val kind = when (details.productId) {
            BillingProductIds.BASIC_MONTHLY -> BillingProductKind.BASIC_SUB
            BillingProductIds.PREMIUM_MONTHLY -> BillingProductKind.PREMIUM_SUB
            BillingProductIds.STORAGE_20GB_MONTHLY -> BillingProductKind.STORAGE_ADDON
            else -> return null
        }
        return BillingProduct(
            productId = details.productId,
            title = details.title,
            description = details.description,
            formattedPrice = price,
            kind = kind,
            offerToken = offer?.offerToken,
        )
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): Result<Unit> =
        mutex.withLock {
            runCatching {
                _error.value = null
                ensureReady()
                // Add-ons require active Premium; 6th pack blocked client-side too.
                if (BillingProductIds.isStorageAddon(productId)) {
                    val user = authRepository.getAuthUser()
                    if (user?.privilegeTier != PrivilegeTier.PREMIUM) {
                        error(PREMIUM_REQUIRED_ADDON_MESSAGE)
                    }
                    val addons = user?.storage?.addonCount ?: 0
                    val max = user?.storage?.maxAddonPacks ?: 5
                    if (addons >= max) {
                        error(MAX_ADDONS_MESSAGE)
                    }
                }
                val details = productDetailsById[productId]
                    ?: error("Product not available from Play yet. Try again after products are published.")
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    ?: error("No subscription offer for $productId")
                val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build()
                _inFlight.value = true
                val result = billingClient.launchBillingFlow(activity, flowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _inFlight.value = false
                    error(result.debugMessage.ifBlank { "Could not start purchase" })
                }
            }.onFailure { e ->
                _inFlight.value = false
                _error.value = e.message
                Log.e(TAG, "launchPurchase failed", e)
            }
        }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch(Dispatchers.IO) {
            try {
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        purchases.orEmpty().forEach { processPurchase(it) }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        // no-op
                    }
                    else -> {
                        _error.value = result.debugMessage.ifBlank { "Purchase failed" }
                    }
                }
            } finally {
                _inFlight.value = false
            }
        }
    }

    private suspend fun processPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        // 1) verify with backend (dry-run accepts any non-empty token)
        val verifiedUser = try {
            billingApi.verifyPurchase(
                VerifyPurchaseRequestDto(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    packageName = context.packageName,
                )
            )
        } catch (e: HttpException) {
            val msg = mapHttpError(e)
            _error.value = msg
            Log.e(TAG, "verify failed: $msg", e)
            return
        } catch (e: Exception) {
            _error.value = e.message ?: "Verify failed"
            Log.e(TAG, "verify failed", e)
            return
        }
        authRepository.applyRemoteUser(AuthMapper.toDomainUser(verifiedUser))

        // 2) device acknowledge
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val ackResult = billingClient.acknowledgePurchase(ackParams)
            if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "BillingClient.acknowledgePurchase: ${ackResult.debugMessage}")
            }
        }

        // 3) server ack
        try {
            billingApi.acknowledgePurchase(
                AckPurchaseRequestDto(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                )
            )
        } catch (e: Exception) {
            // Non-fatal for entitlement (verify already applied); log for Benny.
            Log.w(TAG, "POST /v1/billing/play/ack failed", e)
        }
    }

    override suspend fun restorePurchases(): Result<Unit> = mutex.withLock {
        runCatching {
            _error.value = null
            ensureReady()
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = billingClient.queryPurchasesAsync(params)
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                error(result.billingResult.debugMessage.ifBlank { "Restore failed" })
            }
            result.purchasesList.forEach { processPurchase(it) }
        }.onFailure { e ->
            _error.value = e.message
        }
    }

    private fun mapHttpError(e: HttpException): String {
        val body = try {
            e.response()?.errorBody()?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }
        if (body.contains("requires an active Premium", ignoreCase = true) ||
            body.contains("storage add-on requires", ignoreCase = true)
        ) {
            return PREMIUM_REQUIRED_ADDON_MESSAGE
        }
        if (e.code() == 409 || body.contains("max addon", ignoreCase = true)) {
            return MAX_ADDONS_MESSAGE
        }
        if (e.code() == 402) return "Purchase is not active yet. Try Restore purchases."
        return body.ifBlank { "Billing error (${e.code()})" }
    }


    override suspend fun cancelStorageAddon(addonId: String): Result<Unit> = mutex.withLock {
        runCatching {
            _error.value = null
            require(addonId.isNotBlank()) { "addon_id required" }
            val resp = try {
                billingApi.cancelAddon(CancelAddonRequestDto(addonId = addonId))
            } catch (e: HttpException) {
                throw IllegalStateException(mapHttpError(e), e)
            }
            val userDto = resp.user
                ?: throw IllegalStateException("Cancel succeeded but user payload missing")
            authRepository.applyRemoteUser(AuthMapper.toDomainUser(userDto))
            Unit
        }.onFailure { e ->
            _error.value = e.message
            Log.e(TAG, "cancelStorageAddon failed", e)
        }
    }

    companion object {
        private const val TAG = "BillingRepository"
        const val MAX_ADDONS_MESSAGE =
            "You've reached the maximum of 5 storage packs (+100 GB). Remove or wait for a pack to lapse."
        const val PREMIUM_REQUIRED_ADDON_MESSAGE =
            "Storage add-ons require an active Premium subscription."
    }
}
