package com.aethelsoft.grooveplayer.domain.usecase.transfer

import com.aethelsoft.grooveplayer.domain.model.transfer.Transfer
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    operator fun invoke(): Flow<List<Transfer>> = transferRepository.observeActiveTransfers()
}
