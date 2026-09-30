package com.aethelsoft.grooveplayer.domain.usecase.profile_category

import com.aethelsoft.grooveplayer.domain.model.RecentUpdates
import com.aethelsoft.grooveplayer.domain.repository.RecentUpdatesRepository
import javax.inject.Inject

class GetRecentUpdatesUseCase @Inject constructor(
    private val recentUpdatesRepository: RecentUpdatesRepository,
) {
    operator fun invoke(): RecentUpdates = recentUpdatesRepository.load()
}
