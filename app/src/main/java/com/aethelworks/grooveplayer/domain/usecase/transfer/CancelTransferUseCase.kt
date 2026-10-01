package com.aethelworks.grooveplayer.domain.usecase.transfer

import com.aethelworks.grooveplayer.data.transfer.NearbyTransferManager
import com.aethelworks.grooveplayer.data.transfer.TransferController
import com.aethelworks.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelworks.grooveplayer.domain.repository.transfer.TransferRepository
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
