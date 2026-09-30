package com.aethelsoft.grooveplayer.data.profile

import com.aethelsoft.grooveplayer.domain.model.RecentUpdateItem
import com.aethelsoft.grooveplayer.domain.model.RecentUpdateSection
import com.aethelsoft.grooveplayer.domain.model.RecentUpdates
import org.json.JSONObject

object RecentUpdatesJson {
    fun parse(text: String): RecentUpdates {
        val root = JSONObject(text)
        val sectionsJson = root.getJSONArray("sections")
        val sections = buildList {
            for (index in 0 until sectionsJson.length()) {
                val section = sectionsJson.getJSONObject(index)
                val itemsJson = section.getJSONArray("items")
                val items = buildList {
                    for (itemIndex in 0 until itemsJson.length()) {
                        val item = itemsJson.getJSONObject(itemIndex)
                        add(
                            RecentUpdateItem(
                                title = item.getString("title"),
                                detail = item.getString("detail"),
                            ),
                        )
                    }
                }
                add(
                    RecentUpdateSection(
                        title = section.getString("title"),
                        items = items,
                    ),
                )
            }
        }
        return RecentUpdates(
            title = root.getString("title"),
            intro = root.getString("intro"),
            sections = sections,
        )
    }
}
