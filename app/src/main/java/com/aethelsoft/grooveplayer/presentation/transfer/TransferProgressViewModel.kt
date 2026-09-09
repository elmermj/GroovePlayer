package com.aethelsoft.grooveplayer.presentation.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.transfer.Transfer
import com.aethelsoft.grooveplayer.domain.usecase.transfer.CancelTransferUseCase
import com.aethelsoft.grooveplayer.domain.usecase.transfer.GetActiveTransfersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferProgressViewModel @Inject constructor(
    private val getActiveTransfersUseCase: GetActiveTransfersUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase,
) : ViewModel() {

    val activeTransfers: StateFlow<List<Transfer>> = getActiveTransfersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelTransfer(transferId: Long) {
        viewModelScope.launch {
            cancelTransferUseCase(transferId)
        }
    }
}
