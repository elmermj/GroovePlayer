package com.aethelsoft.grooveplayer.domain.usecase.transfer

import com.aethelsoft.grooveplayer.data.transfer.TransferController
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import javax.inject.Inject

class PauseTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val transferController: TransferController,
) {
    suspend operator fun invoke(transferId: Long) {
        transferController.requestPause(transferId)
        transferRepository.updateTransferStatus(transferId, TransferStatus.PAUSED.name)
    }
}
