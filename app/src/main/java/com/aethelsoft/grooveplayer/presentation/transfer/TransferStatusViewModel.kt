package com.aethelsoft.grooveplayer.presentation.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.transfer.Transfer
import com.aethelsoft.grooveplayer.domain.usecase.transfer.CancelTransferUseCase
import com.aethelsoft.grooveplayer.domain.usecase.transfer.GetActiveTransfersUseCase
import com.aethelsoft.grooveplayer.domain.usecase.transfer.GetTransferHistoryUseCase
import com.aethelsoft.grooveplayer.domain.usecase.transfer.PauseTransferUseCase
import com.aethelsoft.grooveplayer.domain.usecase.transfer.ResumeTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferStatusViewModel @Inject constructor(
    private val getActiveTransfersUseCase: GetActiveTransfersUseCase,
    private val getTransferHistoryUseCase: GetTransferHistoryUseCase,
    private val pauseTransferUseCase: PauseTransferUseCase,
    private val resumeTransferUseCase: ResumeTransferUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase,
) : ViewModel() {

    val activeTransfers: StateFlow<List<Transfer>> = getActiveTransfersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transferHistory: StateFlow<List<Transfer>> = getTransferHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pauseTransfer(transferId: Long) {
        viewModelScope.launch {
            pauseTransferUseCase(transferId)
        }
    }

    fun resumeTransfer(transferId: Long) {
        viewModelScope.launch {
            resumeTransferUseCase(transferId)
        }
    }

    fun cancelTransfer(transferId: Long) {
        viewModelScope.launch {
            cancelTransferUseCase(transferId)
        }
    }
}
