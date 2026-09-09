package com.aethelsoft.grooveplayer.domain.usecase.transfer

import com.aethelsoft.grooveplayer.data.transfer.NearbyTransferManager
import com.aethelsoft.grooveplayer.data.transfer.TransferController
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import javax.inject.Inject

class CancelTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val transferController: TransferController,
    private val nearbyTransferManager: NearbyTransferManager,
) {
    suspend operator fun invoke(transferId: Long) {
        transferController.requestCancel(transferId)
        transferRepository.completeTransfer(transferId, TransferStatus.CANCELLED.name)
        nearbyTransferManager.disconnect()
    }
}
