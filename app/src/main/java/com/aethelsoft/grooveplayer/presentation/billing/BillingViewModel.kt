package com.aethelsoft.grooveplayer.presentation.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.BillingProduct
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelsoft.grooveplayer.domain.model.TrimStrategy
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BillingRepository
import com.aethelsoft.grooveplayer.domain.usecase.auth_category.RestoreAuthSessionUseCase
import com.aethelsoft.grooveplayer.domain.usecase.backup_category.TrimCloudBackupUseCase
import com.aethelsoft.grooveplayer.domain.usecase.billing_category.CancelStorageAddonUseCase
import com.aethelsoft.grooveplayer.domain.usecase.billing_category.EnsureBillingReadyUseCase
import com.aethelsoft.grooveplayer.domain.usecase.billing_category.LaunchPurchaseUseCase
import com.aethelsoft.grooveplayer.domain.usecase.billing_category.ObserveBillingProductsUseCase
import com.aethelsoft.grooveplayer.domain.usecase.billing_category.RestorePurchasesUseCase
import com.aethelsoft.grooveplayer.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    application: Application,
    observeBillingProductsUseCase: ObserveBillingProductsUseCase,
    private val ensureBillingReadyUseCase: EnsureBillingReadyUseCase,
    private val launchPurchaseUseCase: LaunchPurchaseUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val cancelStorageAddonUseCase: CancelStorageAddonUseCase,
    private val trimCloudBackupUseCase: TrimCloudBackupUseCase,
    private val billingRepository: BillingRepository,
    authRepository: AuthRepository,
    private val restoreAuthSessionUseCase: RestoreAuthSessionUseCase,
) : BaseViewModel(application) {

    val products: StateFlow<List<BillingProduct>> =
        observeBillingProductsUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val billingReady: StateFlow<Boolean> =
        billingRepository.observeIsReady()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val purchaseInFlight: StateFlow<Boolean> =
        billingRepository.observePurchaseInFlight()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val billingError: StateFlow<String?> =
        billingRepository.observeError()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val authUser: StateFlow<AuthUser?> =
        authRepository.observeAuthUser()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val privilegeTier: StateFlow<PrivilegeTier> =
        authRepository.observePrivilegeTier()
            .stateIn(viewModelScope, SharingStarted.Eagerly, PrivilegeTier.FREE)

    private val _cancelInFlight = MutableStateFlow(false)
    val cancelInFlight: StateFlow<Boolean> = _cancelInFlight.asStateFlow()

    private val _trimInFlight = MutableStateFlow(false)
    val trimInFlight: StateFlow<Boolean> = _trimInFlight.asStateFlow()

    private val _trimMessage = MutableStateFlow<String?>(null)
    val trimMessage: StateFlow<String?> = _trimMessage.asStateFlow()

    private val _lastTrimResult = MutableStateFlow<TrimCloudBackupResult?>(null)
    val lastTrimResult: StateFlow<TrimCloudBackupResult?> = _lastTrimResult.asStateFlow()

    init {
        viewModelScope.launch {
            ensureBillingReadyUseCase()
            restoreAuthSessionUseCase()
        }
    }

    fun purchase(activity: Activity, productId: String) = viewModelScope.launch {
        launchPurchaseUseCase(activity, productId)
    }

    fun restore() = viewModelScope.launch {
        restorePurchasesUseCase()
        restoreAuthSessionUseCase()
    }

    /** Mid-period: stop renewal only — no trim until period end. */
    fun cancelAddon(addonId: String) = viewModelScope.launch {
        _cancelInFlight.value = true
        try {
            cancelStorageAddonUseCase(addonId)
            restoreAuthSessionUseCase()
        } finally {
            _cancelInFlight.value = false
        }
    }

    fun trimCloud(
        strategy: TrimStrategy,
        artist: String? = null,
        album: String? = null,
        year: Int? = null,
        limitBytes: Long? = null,
    ) = viewModelScope.launch {
        _trimInFlight.value = true
        _trimMessage.value = null
        try {
            val result = trimCloudBackupUseCase(
                TrimCloudBackupRequest(
                    strategy = strategy,
                    artist = artist,
                    album = album,
                    year = year,
                    limitBytes = limitBytes,
                )
            )
            result.onSuccess {
                _lastTrimResult.value = it
                _trimMessage.value =
                    "Removed ${it.deleted} cloud backup file(s) · freed ${it.bytesFreed} bytes"
                restoreAuthSessionUseCase()
            }.onFailure {
                _trimMessage.value = it.message ?: "Could not free cloud space"
            }
        } finally {
            _trimInFlight.value = false
        }
    }

    fun clearError() = billingRepository.clearError()

    fun clearTrimMessage() {
        _trimMessage.value = null
    }

    override fun refresh() {
        viewModelScope.launch {
            ensureBillingReadyUseCase()
            restoreAuthSessionUseCase()
        }
    }
}
