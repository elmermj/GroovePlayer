package com.aethelsoft.grooveplayer.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.data.playback.SongAvailabilityRepository
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.SongAvailabilityMark
import com.aethelsoft.grooveplayer.domain.playback.songAvailabilityContentDescription
import com.aethelsoft.grooveplayer.utils.theme.icons.XCloudOutlined
import com.aethelsoft.grooveplayer.utils.theme.icons.XSmartphone
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Premium availability mark. Local + cloud is phone then cloud; cloud-only is the cloud glyph.
 * Free and Basic render nothing. Local-only renders nothing. Not clickable.
 */
@Composable
fun SongAvailabilityBadge(
    mark: SongAvailabilityMark,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val tint = SoftWhite.copy(alpha = 0.70f)
    val description = songAvailabilityContentDescription(mark)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mark == SongAvailabilityMark.LOCAL_AND_CLOUD) {
            Icon(
                imageVector = XSmartphone,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = tint,
            )
        }
        Icon(
            imageVector = XCloudOutlined,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}

/** Full player: centered under the artist. Nothing when the mark is absent. */
@Composable
fun FullPlayerSongAvailability(song: Song?) {
    if (song == null) return
    FullPlayerSongAvailabilityMark(song)
}

@Composable
private fun FullPlayerSongAvailabilityMark(song: Song) {
    val mark = rememberSongAvailabilityMark(song) ?: return
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        SongAvailabilityBadge(mark = mark, iconSize = 18.dp)
    }
}

@Composable
fun rememberSongAvailabilityMark(song: Song): SongAvailabilityMark? {
    val context = LocalContext.current
    val repository = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                SongAvailabilityEntryPoint::class.java,
            ).songAvailabilityRepository()
        }.getOrNull()
    } ?: return null
    val premiumNow = remember(repository) {
        runCatching { repository.isPremiumNow() }.getOrDefault(false)
    }
    val premium by repository.showAvailability.collectAsState(initial = premiumNow)
    var mark by remember(song.id) { mutableStateOf<SongAvailabilityMark?>(null) }
    LaunchedEffect(premium, song.id, song.uri, song.filePath, song.fileSizeBytes) {
        mark = try {
            if (!premium) null else repository.markFor(song)
        } catch (e: Exception) {
            null
        }
    }
    if (!premium) return null
    return mark
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SongAvailabilityEntryPoint {
    fun songAvailabilityRepository(): SongAvailabilityRepository
}
