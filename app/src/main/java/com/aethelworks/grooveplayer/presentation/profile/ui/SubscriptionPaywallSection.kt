package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
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
import com.aethelworks.grooveplayer.domain.model.BillingProductKind
import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import com.aethelworks.grooveplayer.domain.model.StorageEntitlement
import com.aethelworks.grooveplayer.presentation.backup.ui.AddonCancelSection
import com.aethelworks.grooveplayer.presentation.backup.ui.OverQuotaTrimSection
import com.aethelworks.grooveplayer.presentation.backup.ui.formatPeriodEnd
import com.aethelworks.grooveplayer.presentation.billing.BillingViewModel
import com.aethelworks.grooveplayer.utils.BillingProductIds
import com.aethelworks.grooveplayer.utils.DeviceType
import com.aethelworks.grooveplayer.utils.rememberDeviceType
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.StorageFormatUtils
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

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
        text = stringResource(R.string.sub_title),
        style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
        color = GrooveTheme.colors.onSurface,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = when {
            user == null -> stringResource(R.string.sub_signed_out)
            tier == PrivilegeTier.FREE -> stringResource(R.string.sub_free)
            tier == PrivilegeTier.BASIC -> stringResource(R.string.sub_basic)
            tier == PrivilegeTier.PREMIUM -> stringResource(R.string.sub_premium)
            else -> stringResource(R.string.sub_free)
        },
        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
        color = SoftWhite,
    )

    val renewalDate = formatPeriodEnd(storage?.premiumPeriodEndEpochMs, storage?.premiumPeriodEndIso)
    if (renewalDate != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sub_renews, renewalDate),
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
            text = stringResource(R.string.sub_grace, days.toInt(), hours.toInt()),
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFB74D),
        )
    } else if (storage?.overQuota != true && storage?.readOnly == true) {
        Spacer(Modifier.height(S_PADDING))
        Text(
            text = stringResource(R.string.sub_grace_active),
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
            title = stringResource(R.string.sub_cloud_backup),
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
                title = stringResource(R.string.sub_get_basic, basic.formattedPrice),
                isActive = activity != null && !inFlight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (tier != PrivilegeTier.PREMIUM && premium != null) {
            ProfileSettingsButton(
                onClick = {
                    if (activity != null) billingViewModel.purchase(activity, premium.productId)
                },
                title = stringResource(R.string.sub_get_premium, premium.formattedPrice),
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
                    stringResource(R.string.sub_add_storage, addon.formattedPrice, count, max)
                } else {
                    stringResource(R.string.sub_storage_maxed, count, max)
                },
                isActive = activity != null && !inFlight && canBuy,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.sub_addons_renew),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        ProfileSettingsButton(
            onClick = { billingViewModel.restore() },
            title = stringResource(R.string.sub_restore),
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
        text = stringResource(R.string.sub_quota, StorageFormatUtils.formatBytes(used, total), StorageFormatUtils.formatBytes(total, total)),
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
                text = stringResource(R.string.sub_over_quota),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF5252),
            )
        }
        storage.hardStop -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.sub_hard_stop),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFF5252),
            )
        }
        storage.softWarn -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.sub_soft_warn),
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = Color(0xFFFFB74D),
            )
        }
    }
    val pending = storage.pendingQuotaBytes
    if (pending != null && pending != storage.quotaBytes && !storage.overQuota) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sub_pending, StorageFormatUtils.formatBytes(pending, pending.coerceAtLeast(1L))),
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFFCC80),
        )
    }
}
