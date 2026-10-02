package com.aethelworks.grooveplayer.presentation.share

import android.content.Intent
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.aethelworks.grooveplayer.presentation.common.FlatRow
import com.aethelworks.grooveplayer.presentation.common.OverlineLabel
import com.aethelworks.grooveplayer.utils.theme.ui.PoppinsFontFamily
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

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

    Column(modifier = modifier.fillMaxWidth()) {
        OverlineLabel("Enable features for sharing")
        if (needsNfc) {
            RequirementRow(
                message = "NFC is off — needed for Tap to Share",
                actionLabel = "Turn on NFC",
                onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                showDivider = needsWifi,
            )
        }
        if (needsWifi) {
            RequirementRow(
                message = "Wi‑Fi is off — nearby discovery works better with Wi‑Fi on",
                actionLabel = "Turn on Wi‑Fi",
                onAction = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                showDivider = true,
            )
        }
    }
}

@Composable
private fun RequirementRow(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    showDivider: Boolean,
) {
    FlatRow(
        title = message,
        titleMaxLines = 3,
        trailing = {
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = SoftWhite),
            ) {
                Text(
                    text = actionLabel,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        showDivider = showDivider,
    )
}
