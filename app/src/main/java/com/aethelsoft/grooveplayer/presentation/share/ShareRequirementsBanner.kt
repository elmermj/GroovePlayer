package com.aethelsoft.grooveplayer.presentation.share

import android.content.Intent
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveCardTitle
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveSurfaceCard
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING

data class ShareRequirementsStatus(
    val isNfcAvailable: Boolean,
    val isNfcEnabled: Boolean,
    val isWifiEnabled: Boolean,
)

@Composable
fun rememberShareRequirementsStatus(): ShareRequirementsStatus {
    val context = LocalContext.current
    return remember(context) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val wifiManager = @Suppress("DEPRECATION")
        context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? WifiManager
        ShareRequirementsStatus(
            isNfcAvailable = nfcAdapter != null,
            isNfcEnabled = nfcAdapter?.isEnabled == true,
            isWifiEnabled = run {
                @Suppress("DEPRECATION")
                wifiManager?.isWifiEnabled == true
            }
        )
    }
}

@Composable
fun ShareRequirementsBanner(
    status: ShareRequirementsStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val needsNfc = status.isNfcAvailable && !status.isNfcEnabled
    val needsWifi = !status.isWifiEnabled

    if (!needsNfc && !needsWifi) return

    GrooveSurfaceCard(modifier = modifier) {
        GrooveCardTitle("Enable features for sharing")
        Spacer(modifier = Modifier.height(S_PADDING))
        if (needsNfc) {
            RequirementRow(
                message = "NFC is off — needed for Tap to Share",
                actionLabel = "Turn on NFC",
                onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
            )
        }
        if (needsWifi) {
            if (needsNfc) Spacer(modifier = Modifier.height(S_PADDING))
            RequirementRow(
                message = "Wi‑Fi is off — nearby discovery works better with Wi‑Fi on",
                actionLabel = "Turn on Wi‑Fi",
                onAction = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            )
        }
    }
}

@Composable
private fun RequirementRow(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(XS_PADDING),
    ) {
        GrooveMutedText(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        GrooveActionButton(
            label = actionLabel,
            onClick = onAction,
            isPrimary = true,
        )
    }
}
