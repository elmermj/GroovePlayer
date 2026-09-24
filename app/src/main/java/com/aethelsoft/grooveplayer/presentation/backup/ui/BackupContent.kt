package com.aethelsoft.grooveplayer.presentation.backup.ui

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.AuthUser
import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.model.BillingProduct
import com.aethelsoft.grooveplayer.domain.model.BillingProductKind
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.StorageAddon
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.domain.model.TrimStrategy
import com.aethelsoft.grooveplayer.presentation.backup.BackupObjectsFilter
import com.aethelsoft.grooveplayer.utils.BillingProductIds
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.StorageFormatUtils
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingsButton
import com.aethelsoft.grooveplayer.domain.model.CloudBackupState
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase
import androidx.compose.material3.LinearProgressIndicator

/**
 * Backup screen (Luxi + Jorge/Benny): **Usage**, **Back up now**, **Backed up songs**.
 * 403 `storage.read_only` / hard_stop / over_quota surface clear messages (no crash).
 */
@Composable
fun BackupContent(
    user: AuthUser?,
    tier: PrivilegeTier,
    backupState: CloudBackupState,
    onStartBackup: () -> Unit,
    /** Directory paths included in backup (from Backup settings / exclusions). Paths only — not file lists. */
    includedFolders: List<String> = emptyList(),
    onRefreshIncludedFolders: () -> Unit = {},
    objects: List<BackupObject>,
    objectsLoading: Boolean,
    objectsError: String?,
    objectsFilter: BackupObjectsFilter,
    onFilterChange: (BackupObjectsFilter) -> Unit,
    deleteInFlight: Boolean,
    onDeleteObject: (BackupObject) -> Unit,
    billingProducts: List<BillingProduct>,
    purchaseInFlight: Boolean,
    cancelInFlight: Boolean,
    billingError: String?,
    onPurchaseAddon: (Activity, String) -> Unit,
    onCancelAddon: (addonId: String) -> Unit,
    trimInFlight: Boolean = false,
    trimMessage: String? = null,
    onTrim: (
        strategy: TrimStrategy,
        artist: String?,
        album: String?,
        year: Int?,
        limitBytes: Long?,
    ) -> Unit = { _, _, _, _, _ -> },
    /** Phone/Tablet: collapse Free-cloud-space trim controls by default to protect vertical space. */
    compactTrimStack: Boolean = false,
    librarySnapshot: CloudLibrarySnapshot? = null,
    libraryLoading: Boolean = false,
    restoreInFlight: Boolean = false,
    restoreMessage: String? = null,
    onRestoreLibrary: () -> Unit = {},
) {
    val storage = user?.storage
    // Signed-out must not keep an in-flight Premium backup job on screen.
    val visibleBackupState = if (user == null) backupState.asSignedOut() else backupState
    val context = LocalContext.current
    val activity = context as? Activity
    var pendingCancelAddon by remember { mutableStateOf<StorageAddon?>(null) }
    var pendingDelete by remember { mutableStateOf<BackupObject?>(null) }
    var showConfirmBackup by remember { mutableStateOf(false) }
    LaunchedEffect(showConfirmBackup) {
        if (showConfirmBackup) onRefreshIncludedFolders()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(S_PADDING),
    ) {
        // —— 1. Usage ——
        Text(
            text = "Usage",
            style = GrooveTheme.typography.sectionTitle.toTextStyle(),
            color = GrooveTheme.colors.onSurface,
        )

        Text(
            text = "Cloud backup includes your library data (Room DB snapshot: favourites, " +
                "recents, playlists, settings, metadata) plus audio from included folders. " +
                "Trim and long-press remove songs only — not the library snapshot.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite.copy(alpha = 0.75f),
        )

        if (storage != null) {
            UsageArcMeter(storage = storage)

            SubscriptionSummaryRow(tier = tier, storage = storage)

            AddonStepperRow(
                storage = storage,
                tier = tier,
                addonProduct = billingProducts.firstOrNull { it.kind == BillingProductKind.STORAGE_ADDON }
                    ?: billingProducts.firstOrNull { it.productId == BillingProductIds.STORAGE_20GB_MONTHLY },
                purchaseInFlight = purchaseInFlight,
                cancelInFlight = cancelInFlight,
                activity = activity,
                onIncrease = { productId ->
                    if (activity != null) onPurchaseAddon(activity, productId)
                },
                onDecreaseRequest = { addon -> pendingCancelAddon = addon },
            )

            if (storage.overQuota) {
                OverQuotaTrimSection(
                    storage = storage,
                    trimInFlight = trimInFlight,
                    trimMessage = trimMessage,
                    onTrim = onTrim,
                    compact = compactTrimStack,
                )
            }

            BackupNowSection(
                storage = storage,
                tier = tier,
                backupState = visibleBackupState,
                onStartBackup = { showConfirmBackup = true },
            )
        } else {
            Text(
                text = when {
                    user == null ->
                        "Sign in to manage cloud backup. Quota is included with Premium."
                    tier == PrivilegeTier.PREMIUM ->
                        "Loading cloud quota…"
                    else ->
                        "Cloud backup quota is included with Premium."
                },
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
            BackupNowSection(
                storage = null,
                tier = tier,
                backupState = visibleBackupState,
                onStartBackup = { showConfirmBackup = true },
            )
        }

        RestoreLibrarySection(
            tier = tier,
            librarySnapshot = librarySnapshot,
            libraryLoading = libraryLoading,
            restoreInFlight = restoreInFlight,
            restoreMessage = restoreMessage,
            onRestoreLibrary = onRestoreLibrary,
        )

        if (!billingError.isNullOrBlank()) {
            Text(
                text = billingError,
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF8A80),
            )
        }

        Spacer(Modifier.height(8.dp))

        // —— 2. Backed up songs ——
        BackedUpSongsSection(
            storage = storage,
            objects = objects,
            objectsLoading = objectsLoading,
            objectsError = objectsError,
            objectsFilter = objectsFilter,
            onFilterChange = onFilterChange,
            deleteInFlight = deleteInFlight,
            onLongPress = { pendingDelete = it },
        )
    }

    pendingCancelAddon?.let { addon ->
        AddonDecreasePolicyDialog(
            addon = addon,
            storage = storage,
            onDismiss = { pendingCancelAddon = null },
            onConfirm = {
                onCancelAddon(addon.id)
                pendingCancelAddon = null
            },
        )
    }

    pendingDelete?.let { obj ->
        CloudDeleteConfirmDialog(
            obj = obj,
            deleteInFlight = deleteInFlight,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteObject(obj)
                pendingDelete = null
            },
        )
    }

    if (showConfirmBackup) {
        ConfirmBackupDialog(
            includedFolders = includedFolders,
            onDismiss = { showConfirmBackup = false },
            onConfirm = {
                showConfirmBackup = false
                onStartBackup()
            },
        )
    }
}

@Composable
private fun SubscriptionSummaryRow(
    tier: PrivilegeTier,
    storage: StorageEntitlement,
) {
    val renewal = formatPeriodEnd(storage.premiumPeriodEndEpochMs, storage.premiumPeriodEndIso)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = when (tier) {
                PrivilegeTier.FREE -> "Free · ads on"
                PrivilegeTier.BASIC -> "Basic · ads off"
                PrivilegeTier.PREMIUM -> "Premium · ads off + backup"
            },
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
        )
        if (renewal != null) {
            Text(
                text = "Renews · $renewal (Premium + add-ons)",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite.copy(alpha = 0.8f),
            )
        }
        val pending = storage.pendingQuotaBytes
        if (pending != null && pending != storage.quotaBytes) {
            Text(
                text = "After renewals drop · cloud quota → ${
                    StorageFormatUtils.formatBytes(pending, pending.coerceAtLeast(1L))
                }",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFCC80),
            )
        }
    }
}

@Composable
private fun AddonStepperRow(
    storage: StorageEntitlement,
    tier: PrivilegeTier,
    addonProduct: BillingProduct?,
    purchaseInFlight: Boolean,
    cancelInFlight: Boolean,
    activity: Activity?,
    onIncrease: (productId: String) -> Unit,
    onDecreaseRequest: (StorageAddon) -> Unit,
) {
    if (tier != PrivilegeTier.PREMIUM) return

    val count = storage.addonCount
    val max = storage.maxAddonPacks
    val activeAddons = storage.addons.filter { !it.cancelAtPeriodEnd }
    val canIncrease = count < max &&
        storage.readOnly != true &&
        activity != null &&
        addonProduct != null &&
        !purchaseInFlight
    val canDecrease = activeAddons.isNotEmpty() && !cancelInFlight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        GrooveTheme.colors.surface.copy(alpha = 0.9f),
                        GrooveTheme.colors.surface.copy(alpha = 0.55f),
                    ),
                ),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Storage add-ons",
            style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
            color = GrooveTheme.colors.onSurface,
        )
        Text(
            text = "+20 GB packs · $count/$max active",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperChip(
                label = "−",
                enabled = canDecrease,
                onClick = { activeAddons.lastOrNull()?.let(onDecreaseRequest) },
            )
            Text(
                text = "$count",
                style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
                color = GrooveTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            StepperChip(
                label = "+",
                enabled = canIncrease,
                onClick = {
                    addonProduct?.productId?.let(onIncrease)
                },
            )
            Spacer(Modifier.weight(1f))
            if (addonProduct != null) {
                Text(
                    text = addonProduct.formattedPrice,
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                    color = SoftWhite.copy(alpha = 0.85f),
                )
            }
        }

        storage.addons.filter { it.cancelAtPeriodEnd }.forEach { addon ->
            val whenLabel = formatPeriodEnd(
                addon.expiresAtEpochMs ?: storage.premiumPeriodEndEpochMs,
                addon.expiresAtIso ?: storage.premiumPeriodEndIso,
            )
            Text(
                text = if (whenLabel != null) {
                    "+${addon.packGb} GB cancels on $whenLabel — full quota kept until then"
                } else {
                    "+${addon.packGb} GB cancels at period end — full quota kept until then"
                },
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFB74D),
            )
        }
    }
}

@Composable
private fun StepperChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (enabled) GrooveTheme.colors.accent.copy(alpha = 0.85f)
                else GrooveTheme.colors.surfaceRaised.copy(alpha = 0.5f),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) GrooveTheme.colors.onSurface else SoftWhite.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackedUpSongsSection(
    storage: StorageEntitlement?,
    objects: List<BackupObject>,
    objectsLoading: Boolean,
    objectsError: String?,
    objectsFilter: BackupObjectsFilter,
    onFilterChange: (BackupObjectsFilter) -> Unit,
    deleteInFlight: Boolean,
    onLongPress: (BackupObject) -> Unit,
) {
    var filterExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Backed up songs",
            style = GrooveTheme.typography.sectionTitle.toTextStyle(),
            color = GrooveTheme.colors.onSurface,
        )
        Box {
            TextButton(onClick = { filterExpanded = true }) {
                Text(
                    text = "Filter ▾",
                    color = SoftWhite,
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                )
            }
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false },
            ) {
                BackupObjectsFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = filter.label + if (filter == objectsFilter) " ✓" else "",
                            )
                        },
                        onClick = {
                            onFilterChange(filter)
                            filterExpanded = false
                        },
                    )
                }
            }
        }
    }

    // Subtle long-press affordance (cloud-only copy; does not eat per-row estate).
    if (objects.isNotEmpty()) {
        Text(
            text = "Hold to remove from cloud backup",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }

    val listBytes = objects.sumOf { it.sizeBytes }
    val overByList = storage != null &&
        storage.quotaBytes > 0L &&
        listBytes > storage.quotaBytes
    if (storage?.overQuota == true || overByList) {
        Text(
            text = "Over cloud quota — free cloud space or remove songs from cloud backup. " +
                "Local library is untouched.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFF8A80),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33FF5252))
                .padding(12.dp),
        )
    }

    when {
        objectsLoading && objects.isEmpty() -> {
            Text(
                text = "Loading cloud backups…",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        !objectsError.isNullOrBlank() && objects.isEmpty() -> {
            Text(
                text = objectsError,
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF8A80),
            )
        }
        objects.isEmpty() -> {
            Text(
                text = "No songs in cloud backup yet.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        else -> {
            objects.forEach { obj ->
                BackupObjectRow(
                    obj = obj,
                    enabled = !deleteInFlight,
                    onLongPress = { onLongPress(obj) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackupObjectRow(
    obj: BackupObject,
    enabled: Boolean,
    onLongPress: () -> Unit,
) {
    val title = obj.logicalPath?.substringAfterLast('/')
        ?: obj.r2Key.substringAfterLast('/').ifBlank { obj.id }
    val subtitle = buildString {
        append(StorageFormatUtils.formatBytes(obj.sizeBytes, obj.sizeBytes.coerceAtLeast(1L)))
        obj.createdAtIso?.takeIf { it.isNotBlank() }?.let {
            append(" · ")
            append(it.take(10))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrooveTheme.colors.surface.copy(alpha = 0.55f))
            .combinedClickable(
                enabled = enabled,
                onClick = {},
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
            color = GrooveTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddonDecreasePolicyDialog(
    addon: StorageAddon,
    storage: StorageEntitlement?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val whenLabel = formatPeriodEnd(
        addon.expiresAtEpochMs ?: storage?.premiumPeriodEndEpochMs,
        addon.expiresAtIso ?: storage?.premiumPeriodEndIso,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrooveTheme.colors.surface,
        titleContentColor = GrooveTheme.colors.onSurface,
        textContentColor = SoftWhite.copy(alpha = 0.85f),
        title = { Text("Reduce storage pack?") },
        text = {
            Text(
                if (whenLabel != null) {
                    "This cancels the +${addon.packGb} GB pack at period end ($whenLabel). " +
                        "Quota may drop then; if still over cloud quota you may need to trim cloud backup. " +
                        "Local music is never deleted."
                } else {
                    "This cancels the +${addon.packGb} GB pack at period end. " +
                        "Quota may drop then; if still over cloud quota you may need to trim cloud backup. " +
                        "Local music is never deleted."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes", color = GrooveTheme.colors.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No", color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}

@Composable
private fun CloudDeleteConfirmDialog(
    obj: BackupObject,
    deleteInFlight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = obj.logicalPath?.substringAfterLast('/')
        ?: obj.r2Key.substringAfterLast('/').ifBlank { obj.id }
    AlertDialog(
        onDismissRequest = { if (!deleteInFlight) onDismiss() },
        containerColor = GrooveTheme.colors.surface,
        titleContentColor = GrooveTheme.colors.onSurface,
        textContentColor = SoftWhite.copy(alpha = 0.85f),
        title = { Text("Remove from cloud backup?") },
        text = {
            Text(
                "“$title” will be removed from cloud backup only. " +
                    "Your local library is not deleted.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !deleteInFlight,
            ) {
                Text(
                    if (deleteInFlight) "Removing…" else "Remove from cloud",
                    color = GrooveTheme.colors.onSurface,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleteInFlight) {
                Text("Cancel", color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}



@Composable
private fun ConfirmBackupDialog(
    includedFolders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrooveTheme.colors.surface,
        titleContentColor = GrooveTheme.colors.onSurface,
        textContentColor = SoftWhite.copy(alpha = 0.85f),
        title = { Text("Confirm backup") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Library data (Room DB) and audio from these folders will upload to cloud backup. " +
                        "Songs already on cloud (same filename + size) are skipped.",
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                    color = SoftWhite.copy(alpha = 0.85f),
                )
                Text(
                    text = "Included directories",
                    style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
                    color = GrooveTheme.colors.onSurface,
                )
                if (includedFolders.isEmpty()) {
                    Text(
                        text = "No included folders yet — check Excluded folders in Storage settings.",
                        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                        color = SoftWhite.copy(alpha = 0.7f),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        includedFolders.forEach { path ->
                            Text(
                                text = path,
                                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                                color = SoftWhite.copy(alpha = 0.9f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Back up", color = GrooveTheme.colors.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SoftWhite.copy(alpha = 0.65f))
            }
        },
    )
}

@Composable
private fun BackupNowSection(
    storage: StorageEntitlement?,
    tier: PrivilegeTier,
    backupState: CloudBackupState,
    onStartBackup: () -> Unit,
) {
    val phase = backupState.phase
    val busy = phase == CloudBackupPhase.PREPARING || phase == CloudBackupPhase.UPLOADING
    val readOnlyGrace = storage?.readOnly == true && storage.overQuota != true
    val blockedQuota = storage?.overQuota == true || storage?.hardStop == true
    val canStart = tier == PrivilegeTier.PREMIUM &&
        storage != null &&
        !storage.isOptimisticStub &&
        storage.readOnly != true &&
        storage.overQuota != true &&
        storage.hardStop != true &&
        !busy

    Spacer(Modifier.height(8.dp))
    Text(
        text = "Back up now",
        style = GrooveTheme.typography.sectionTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )

    when {
        storage?.isOptimisticStub == true -> {
            Text(
                text = "Cloud quota is a local stub — refresh account before backing up. " +
                    "Real PUTs need live /v1/me.storage (dry_run false).",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFCC80),
            )
        }
        readOnlyGrace -> {
            Text(
                text = "Premium grace — cloud backups are read-only until you resubscribe. " +
                    "Downloads and existing objects stay available. Local library is untouched.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFB74D),
            )
        }
        storage?.overQuota == true -> {
            Text(
                text = "Over cloud quota — new uploads blocked. Use Free cloud space above. " +
                    "Local library is untouched.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF8A80),
            )
        }
        storage?.hardStop == true -> {
            Text(
                text = "Storage full (hard_stop) — new cloud backups blocked. " +
                    "Free cloud space or buy a +20 GB pack.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF8A80),
            )
        }
        tier != PrivilegeTier.PREMIUM -> {
            Text(
                text = "Cloud backup requires Premium.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        storage == null -> {
            Text(
                text = "Sign in with Premium to back up included folders to the cloud.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        else -> {
            Text(
                text = "Uploads library data (Room DB) plus included-folder audio to Cloudflare R2 " +
                "(real PUT when backend dry_run is false).",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite.copy(alpha = 0.85f),
            )
        }
    }

    val buttonEnabled = canStart ||
        (backupState.canRetry && !busy && !blockedQuota && !readOnlyGrace &&
            tier == PrivilegeTier.PREMIUM && storage != null && !storage.isOptimisticStub)
    Spacer(Modifier.height(8.dp))
    ProfileSettingsButton(
        onClick = { if (buttonEnabled) onStartBackup() },
        title = when {
            busy -> "Backing up… ${backupState.progressPercent}%"
            backupState.canRetry -> "Retry upload"
            else -> "Back up now"
        },
        isActive = buttonEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    if (busy) {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (backupState.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = GrooveTheme.colors.accent,
            trackColor = GrooveTheme.colors.surface,
        )
    }

    val status = backupState.message ?: backupState.lastError
    if (!status.isNullOrBlank()) {
        Spacer(Modifier.height(6.dp))
        val color = when (phase) {
            CloudBackupPhase.SUCCESS -> SoftWhite
            CloudBackupPhase.ERROR,
            CloudBackupPhase.BLOCKED_QUOTA,
            CloudBackupPhase.BLOCKED_GRACE,
            CloudBackupPhase.BLOCKED_NOT_PREMIUM -> Color(0xFFFF8A80)
            else -> SoftWhite.copy(alpha = 0.9f)
        }
        Text(
            text = status,
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = color,
        )
    }
    if (backupState.filesSkipped > 0 &&
        (busy || phase == CloudBackupPhase.SUCCESS)
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${backupState.filesSkipped} song(s) skipped — already on cloud (filename + size)",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite.copy(alpha = 0.75f),
        )
    }
    if (backupState.lastRunDryRun) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Last run used dry_run URLs (catalog only — R2 PUT skipped). " +
                "With R2_DRY_RUN=0 the client performs real PUTs.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFCC80),
        )
    }
}


@Composable
private fun RestoreLibrarySection(
    tier: PrivilegeTier,
    librarySnapshot: CloudLibrarySnapshot?,
    libraryLoading: Boolean,
    restoreInFlight: Boolean,
    restoreMessage: String?,
    onRestoreLibrary: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Restore library",
        style = GrooveTheme.typography.sectionTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )
    val hasSnapshot = librarySnapshot != null
    val emptyHint = when {
        tier != PrivilegeTier.PREMIUM ->
            "Premium required to restore cloud library data."
        libraryLoading ->
            "Checking cloud library snapshot…"
        else ->
            "No cloud library snapshot yet — run Back up now first. " +
                "(GET /v1/backup/library returns library: null until the first room_db upload.)"
    }
    Text(
        text = if (hasSnapshot) {
            val schema = librarySnapshot?.schemaVersion?.toString() ?: "?"
            val size = librarySnapshot?.sizeBytes ?: 0L
            "Cloud Room DB ready · schema $schema · ${StorageFormatUtils.formatBytes(size, size.coerceAtLeast(1L))}. " +
                "Restores favourites/recents/playlists/settings. Force-stop the app after restore."
        } else {
            emptyHint
        },
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = SoftWhite.copy(alpha = 0.85f),
    )
    if (!restoreMessage.isNullOrBlank()) {
        Text(
            text = restoreMessage,
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = if (restoreMessage.contains("restored", ignoreCase = true)) {
                Color(0xFF81C784)
            } else {
                Color(0xFFFFCC80)
            },
        )
    }
    Spacer(Modifier.height(8.dp))
    val enabled = hasSnapshot && !restoreInFlight && !libraryLoading && tier == PrivilegeTier.PREMIUM
    ProfileSettingsButton(
        onClick = { if (enabled) onRestoreLibrary() },
        title = when {
            restoreInFlight -> "Restoring library…"
            !hasSnapshot -> "Restore unavailable"
            else -> "Restore library from cloud"
        },
        isActive = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Drop in-flight Premium job chrome once the account session is gone. */
private fun CloudBackupState.asSignedOut(): CloudBackupState = copy(
    phase = CloudBackupPhase.IDLE,
    progressPercent = 0,
    bytesPrepared = 0L,
    bytesUploaded = 0L,
    filesTotal = 0,
    filesCompleted = 0,
    filesDeduped = 0,
    filesSkipped = 0,
    message = null,
    canRetry = false,
    lastError = null,
    lastRunDryRun = false,
)
