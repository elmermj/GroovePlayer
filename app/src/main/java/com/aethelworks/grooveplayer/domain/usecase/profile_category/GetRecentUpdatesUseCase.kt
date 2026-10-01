package com.aethelworks.grooveplayer.domain.usecase.profile_category

import com.aethelworks.grooveplayer.domain.model.RecentUpdates
import com.aethelworks.grooveplayer.domain.repository.RecentUpdatesRepository
import javax.inject.Inject

class GetRecentUpdatesUseCase @Inject constructor(
    private val recentUpdatesRepository: RecentUpdatesRepository,
) {
    operator fun invoke(): RecentUpdates = recentUpdatesRepository.load()
}
