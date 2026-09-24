package com.aethelsoft.grooveplayer.presentation.profile.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.BillingProductKind
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.presentation.backup.ui.AddonCancelSection
import com.aethelsoft.grooveplayer.presentation.backup.ui.OverQuotaTrimSection
import com.aethelsoft.grooveplayer.presentation.backup.ui.formatPeriodEnd
import com.aethelsoft.grooveplayer.presentation.billing.BillingViewModel
import com.aethelsoft.grooveplayer.utils.BillingProductIds
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.rememberDeviceType
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.StorageFormatUtils
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

/**
 * Subscription management + paywall for Profile Account section.
 * Shows one shared Premium/add-on renewal date from `/v1/me.storage.premium_period_end`.
 * Mid-period cancel → “cancels on {period end}” (no trim). Trim only when over_quota.
 */
@Composable
fun SubscriptionPaywallSection(
    onNavigateToBackup: () -> Unit = {},
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val products by billingViewModel.products.collectAsState()
    val tier by billingViewModel.privilegeTier.collectAsState()
    val user by billingViewModel.authUser.collectAsState()
    val inFlight by billingViewModel.purchaseInFlight.collectAsState()
    val cancelInFlight by billingViewModel.cancelInFlight.collectAsState()
    val trimInFlight by billingViewModel.trimInFlight.collectAsState()
    val trimMessage by billingViewModel.trimMessage.collectAsState()
    val error by billingViewModel.billingError.collectAsState()
    val context = LocalContext.current
    // Match Backup: collapse trim strategies on Phone/Tablet; LargeTablet stays expanded.
    val compactTrimStack = rememberDeviceType() != DeviceType.LARGE_TABLET
    val activity = context as? Activity

    val storage = user?.storage

    Spacer(Modifier.height(S_PADDING))
    Text(
        text = "Subscription",
        style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = when {
            user == null -> "Signed out · Free · ads on"
            tier == PrivilegeTier.FREE -> "Free · ads on"
            tier == PrivilegeTier.BASIC -> "Basic · ads off"
            tier == PrivilegeTier.PREMIUM -> "Premium · ads off + backup"
            else -> "Free · ads on"
        },
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = SoftWhite,
    )

    val renewalDate = formatPeriodEnd(storage?.premiumPeriodEndEpochMs, storage?.premiumPeriodEndIso)
    if (renewalDate != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Renews · $renewalDate (Premium + add-ons)",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = SoftWhite,
        )
    }

    // Premium cancel grace (not over-quota trim grace)
    val graceMs = storage?.graceUntilEpochMs
    if (storage?.overQuota != true && storage?.readOnly == true && graceMs != null) {
        Spacer(Modifier.height(S_PADDING))
        val remaining = (graceMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val days = remaining / (24 * 60 * 60 * 1000L)
        val hours = (remaining % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)
        Text(
            text = "Grace period: ${days}d ${hours}h left — backups read-only. After grace, cloud backup is wiped.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFB74D),
        )
    } else if (storage?.overQuota != true && storage?.readOnly == true) {
        Spacer(Modifier.height(S_PADDING))
        Text(
            text = "Grace period active — backups read-only until you resubscribe.",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFB74D),
        )
    }

    if (tier == PrivilegeTier.PREMIUM && storage != null) {
        Spacer(Modifier.height(S_PADDING))
        QuotaBar(storage)
        Spacer(Modifier.height(S_PADDING))
        ProfileSettingsButton(
            onClick = onNavigateToBackup,
            title = "Cloud backup",
            modifier = Modifier.fillMaxWidth(),
        )

        AddonCancelSection(
            storage = storage,
            cancelInFlight = cancelInFlight,
            onCancelAddon = { billingViewModel.cancelAddon(it) },
        )

        // Trim UI only when over_quota (after period-end drop) — never on cancel day.
        OverQuotaTrimSection(
            storage = storage,
            trimInFlight = trimInFlight,
            trimMessage = trimMessage,
            onTrim = { strategy, artist, album, year, limitBytes ->
                billingViewModel.trimCloud(strategy, artist, album, year, limitBytes)
            },
            compact = compactTrimStack,
        )
    }

    Spacer(Modifier.height(S_PADDING))
    Column(verticalArrangement = Arrangement.spacedBy(S_PADDING)) {
        val basic = products.firstOrNull { it.kind == BillingProductKind.BASIC_SUB }
            ?: products.firstOrNull { it.productId == BillingProductIds.BASIC_MONTHLY }
        val premium = products.firstOrNull { it.kind == BillingProductKind.PREMIUM_SUB }
            ?: products.firstOrNull { it.productId == BillingProductIds.PREMIUM_MONTHLY }
        val addon = products.firstOrNull { it.kind == BillingProductKind.STORAGE_ADDON }
            ?: products.firstOrNull { it.productId == BillingProductIds.STORAGE_20GB_MONTHLY }

        if (tier == PrivilegeTier.FREE && basic != null) {
            ProfileSettingsButton(
                onClick = {
                    if (activity != null) billingViewModel.purchase(activity, basic.productId)
                },
                title = "Get Basic · ${basic.formattedPrice}",
                isActive = activity != null && !inFlight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (tier != PrivilegeTier.PREMIUM && premium != null) {
            ProfileSettingsButton(
                onClick = {
                    if (activity != null) billingViewModel.purchase(activity, premium.productId)
                },
                title = "Get Premium · ${premium.formattedPrice}",
                isActive = activity != null && !inFlight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (tier == PrivilegeTier.PREMIUM && addon != null) {
            val count = storage?.addonCount ?: 0
            val max = storage?.maxAddonPacks ?: 5
            val canBuy = count < max && storage?.readOnly != true && storage?.overQuota != true
            ProfileSettingsButton(
                onClick = {
                    if (activity != null) billingViewModel.purchase(activity, addon.productId)
                },
                title = if (canBuy) {
                    "Add +20 GB · ${addon.formattedPrice} ($count/$max)"
                } else {
                    "Storage packs maxed ($count/$max)"
                },
                isActive = activity != null && !inFlight && canBuy,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Add-ons renew on the same date as Premium.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        ProfileSettingsButton(
            onClick = { billingViewModel.restore() },
            title = "Restore purchases",
            isInverse = true,
            isActive = !inFlight,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (!error.isNullOrBlank()) {
        Spacer(Modifier.height(S_PADDING))
        Text(
            text = error.orEmpty(),
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFF8A80),
        )
    }
}

@Composable
fun QuotaBar(storage: StorageEntitlement) {
    val total = storage.quotaBytes.coerceAtLeast(1L)
    val used = storage.usedBytes.coerceAtLeast(0L)
    val fraction = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val barColor = when {
        storage.overQuota || storage.hardStop -> Color(0xFFFF5252)
        storage.softWarn -> Color(0xFFFFB74D)
        else -> GrooveTheme.colors.accent
    }
    Text(
        text = "Backup quota · ${StorageFormatUtils.formatBytes(used, total)} / ${StorageFormatUtils.formatBytes(total, total)}",
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = SoftWhite,
    )
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = barColor,
        trackColor = GrooveTheme.colors.surface,
    )
    when {
        storage.overQuota -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Over cloud quota — free cloud space or wait for auto-trim. Local library is untouched.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF5252),
            )
        }
        storage.hardStop -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Storage full — new cloud backups blocked.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF5252),
            )
        }
        storage.softWarn -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Warning: over 80% of backup quota used.",
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFB74D),
            )
        }
    }
    val pending = storage.pendingQuotaBytes
    if (pending != null && pending != storage.quotaBytes && !storage.overQuota) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "After renewals drop · cloud quota → ${StorageFormatUtils.formatBytes(pending, pending.coerceAtLeast(1L))}",
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFCC80),
        )
    }
}
