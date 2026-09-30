package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.RecentUpdates

interface RecentUpdatesRepository {
    fun load(): RecentUpdates
}
