package com.aethelworks.grooveplayer.data.profile

import android.content.Context
import com.aethelworks.grooveplayer.domain.model.RecentUpdates
import com.aethelworks.grooveplayer.domain.repository.RecentUpdatesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentUpdatesCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecentUpdatesRepository {
    override fun load(): RecentUpdates {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        return RecentUpdatesJson.parse(text)
    }

    private companion object {
        const val ASSET = "recent_updates.json"
    }
}
