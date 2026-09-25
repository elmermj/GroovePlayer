package com.aethelsoft.grooveplayer.domain.playback

/**
 * Premium-only availability badge.
 * Local-only and every non-premium tier render nothing (no spacer).
 */
enum class SongAvailabilityMark {
    LOCAL_AND_CLOUD,
    CLOUD_ONLY,
}

fun songAvailabilityMark(
    isPremium: Boolean,
    localAvailable: Boolean,
    cloudAvailable: Boolean,
): SongAvailabilityMark? {
    if (!isPremium) return null
    return when {
        localAvailable && cloudAvailable -> SongAvailabilityMark.LOCAL_AND_CLOUD
        !localAvailable && cloudAvailable -> SongAvailabilityMark.CLOUD_ONLY
        else -> null
    }
}

fun songAvailabilityContentDescription(mark: SongAvailabilityMark): String = when (mark) {
    SongAvailabilityMark.LOCAL_AND_CLOUD -> "Available on device and in cloud"
    SongAvailabilityMark.CLOUD_ONLY -> "Available in cloud only"
}
