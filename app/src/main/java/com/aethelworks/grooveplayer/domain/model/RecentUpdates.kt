package com.aethelworks.grooveplayer.domain.model

data class RecentUpdates(
    val title: String,
    val intro: String,
    val sections: List<RecentUpdateSection>,
)

data class RecentUpdateSection(
    val title: String,
    val items: List<RecentUpdateItem>,
)

data class RecentUpdateItem(
    val title: String,
    val detail: String,
)
