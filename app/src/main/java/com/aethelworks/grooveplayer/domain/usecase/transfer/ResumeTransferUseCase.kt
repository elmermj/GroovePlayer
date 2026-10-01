package com.aethelworks.grooveplayer.domain.usecase.transfer

import com.aethelworks.grooveplayer.data.transfer.TransferController
import com.aethelworks.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelworks.grooveplayer.domain.repository.transfer.TransferRepository
import javax.inject.Inject

class ResumeTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val transferController: TransferController,
) {
    suspend operator fun invoke(transferId: Long) {
        transferController.requestResume(transferId)
        transferRepository.updateTransferStatus(transferId, TransferStatus.TRANSFERRING.name)
    }
}
