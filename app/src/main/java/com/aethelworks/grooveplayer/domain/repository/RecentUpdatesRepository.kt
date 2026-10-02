package com.aethelworks.grooveplayer.domain.repository

import com.aethelworks.grooveplayer.domain.model.RecentUpdates

interface RecentUpdatesRepository {
    fun load(): RecentUpdates
}
