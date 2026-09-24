package com.aethelsoft.grooveplayer.presentation.backup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.StorageAddon
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.domain.model.TrimStrategy
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingsButton
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.StorageFormatUtils
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import java.text.DateFormat
import java.util.Date

/**
 * Mid-period add-on cancel UI — stop renewal only.
 * Shows “cancels on {premium_period_end}”; **no trim** until over_quota after period end.
 * Copy is cloud-quota only (does not imply local library deletion).
 */
@Composable
fun AddonCancelSection(
    storage: StorageEntitlement,
    cancelInFlight: Boolean,
    onCancelAddon: (addonId: String) -> Unit,
) {
    if (storage.addons.isEmpty()) return

    Spacer(Modifier.height(S_PADDING))
    Text(
        text = "Storage packs",
        style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )
    Text(
        text = "Cancel stops renewal only. You keep full cloud quota until the period ends.",
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = SoftWhite,
    )

    val cancelsOn = formatPeriodEnd(
        storage.premiumPeriodEndEpochMs,
        storage.premiumPeriodEndIso,
    )

    storage.addons.forEach { addon ->
        Spacer(Modifier.height(S_PADDING))
        AddonCancelRow(
            addon = addon,
            cancelsOnLabel = cancelsOn,
            cancelInFlight = cancelInFlight,
            onCancel = { onCancelAddon(addon.id) },
        )
    }

    val pending = storage.pendingQuotaBytes
    if (pending != null && pending != storage.quotaBytes) {
        Spacer(Modifier.height(S_PADDING))
        val pendingLabel = StorageFormatUtils.formatBytes(pending, pending.coerceAtLeast(1L))
        val currentLabel = StorageFormatUtils.formatBytes(
            storage.quotaBytes,
            storage.quotaBytes.coerceAtLeast(1L),
        )
        Text(
            text = "After period end · cloud quota drops to $pendingLabel (now $currentLabel). " +
                "Trim is only needed if usage is still over that quota.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFCC80),
        )
    }
}

@Composable
private fun AddonCancelRow(
    addon: StorageAddon,
    cancelsOnLabel: String?,
    cancelInFlight: Boolean,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "+${addon.packGb} GB · ${addon.status}",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
        )
        if (addon.cancelAtPeriodEnd) {
            Text(
                text = if (cancelsOnLabel != null) {
                    "Cancels on $cancelsOnLabel — full cloud quota kept until then"
                } else {
                    "Cancels at period end — full cloud quota kept until then"
                },
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFB74D),
            )
        } else {
            ProfileSettingsButton(
                onClick = onCancel,
                title = "Cancel pack renewal",
                isInverse = true,
                isActive = !cancelInFlight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Over-quota trim UI — **only** when [StorageEntitlement.overQuota].
 * CLOUD ONLY: removes whole songs/objects from cloud backup (never partial file delete;
 * never implies local library deletion). Optional [limit_bytes] is a soft target —
 * may free a bit more than requested because deletes are whole-object.
 */
@Composable
fun OverQuotaTrimSection(
    storage: StorageEntitlement,
    trimInFlight: Boolean,
    trimMessage: String?,
    onTrim: (
        strategy: TrimStrategy,
        artist: String?,
        album: String?,
        year: Int?,
        limitBytes: Long?,
    ) -> Unit,
    /**
     * When true (Phone/Tablet), collapse strategy controls by default and use denser spacing
     * so the 270° meter and Backed up songs are not crowded. LargeTablet keeps expanded.
     */
    compact: Boolean = false,
) {
    if (!storage.overQuota) return

    val strategies = TrimStrategy.fromSuggested(storage.suggestedStrategies)
    var selected by remember { mutableStateOf(strategies.firstOrNull() ?: TrimStrategy.LATEST) }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }
    var limitMbText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(!compact) }

    val gap = if (compact) 4.dp else S_PADDING

    Spacer(Modifier.height(if (compact) 8.dp else S_PADDING))
    Text(
        text = "Free cloud space",
        style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )
    Text(
        text = if (compact) {
            "Over cloud quota — uploads blocked. Removes whole songs from cloud only; local library untouched."
        } else {
            "Cloud backup is over quota. New uploads are blocked until you free cloud space. " +
                "Removes whole songs from cloud backup; may free a bit more than requested. " +
                "Local library is untouched."
        },
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = Color(0xFFFF8A80),
    )

    val need = storage.bytesToFree.coerceAtLeast(0L)
    if (need > 0L) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Need to free · ${StorageFormatUtils.formatBytes(need, need)} from cloud",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
        )
    }

    val trimUntilMs = storage.graceTrimUntilEpochMs
    if (trimUntilMs != null) {
        val remaining = (trimUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val days = remaining / (24 * 60 * 60 * 1000L)
        val hours = (remaining % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Auto-trim newest from cloud in ${days}d ${hours}h if still over quota.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFB74D),
        )
    }

    if (compact) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "Hide trim options ▴" else "Trim options ▾",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite.copy(alpha = 0.85f),
            )
            if (!expanded) {
                Text(
                    text = selected.label,
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                    color = SoftWhite.copy(alpha = 0.55f),
                )
            }
        }
    }

    if (!expanded) {
        if (!trimMessage.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = trimMessage,
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        return
    }

    Spacer(Modifier.height(gap))
    Text(
        text = "Remove from cloud backup using:",
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = SoftWhite,
    )
    strategies.chunked(2).forEach { row ->
        Spacer(Modifier.height(4.dp))
        row.forEach { strategy ->
            ProfileSettingsButton(
                onClick = { selected = strategy },
                title = strategy.label + if (selected == strategy) " ✓" else "",
                isInverse = selected != strategy,
                isActive = !trimInFlight,
                modifier = Modifier.fillMaxWidth(),
            )
            if (row.last() != strategy) Spacer(Modifier.height(4.dp))
        }
    }

    when (selected) {
        TrimStrategy.BY_ARTIST -> {
            Spacer(Modifier.height(gap))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Artist (cloud filter)") },
            )
        }
        TrimStrategy.BY_ALBUM -> {
            Spacer(Modifier.height(gap))
            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Album (cloud filter)") },
            )
        }
        TrimStrategy.BY_YEAR -> {
            Spacer(Modifier.height(gap))
            OutlinedTextField(
                value = yearText,
                onValueChange = { yearText = it.filter { ch -> ch.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Year (cloud filter)") },
            )
        }
        else -> Unit
    }

    Spacer(Modifier.height(gap))
    OutlinedTextField(
        value = limitMbText,
        onValueChange = { limitMbText = it.filter { ch -> ch.isDigit() }.take(8) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = {
            Text(
                if (compact) {
                    "Optional soft target (MB) — whole songs; blank = until under quota"
                } else {
                    "Optional soft step target (MB) — whole songs only; may free a bit more. " +
                        "Leave blank to free until under quota"
                },
            )
        },
    )

    Spacer(Modifier.height(gap))
    ProfileSettingsButton(
        onClick = {
            val year = yearText.toIntOrNull()
            val limitBytes = limitMbText.toLongOrNull()?.takeIf { it > 0L }?.times(1024L * 1024L)
            onTrim(
                selected,
                artist.takeIf { it.isNotBlank() },
                album.takeIf { it.isNotBlank() },
                year,
                limitBytes,
            )
        },
        title = if (trimInFlight) {
            "Freeing cloud space…"
        } else {
            "Remove from cloud backup"
        },
        isActive = !trimInFlight && when (selected) {
            TrimStrategy.BY_ARTIST -> artist.isNotBlank()
            TrimStrategy.BY_ALBUM -> album.isNotBlank()
            TrimStrategy.BY_YEAR -> yearText.toIntOrNull() != null
            else -> true
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (!trimMessage.isNullOrBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = trimMessage,
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
        )
    }
}

fun formatPeriodEnd(epochMs: Long?, iso: String?): String? {
    if (epochMs != null && epochMs > 0L) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs))
    }
    if (!iso.isNullOrBlank()) return iso
    return null
}
