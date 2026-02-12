package com.aethelsoft.grooveplayer.presentation.share

import android.content.Intent
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A))
            .padding(16.dp)
    ) {
        Text(
            text = "Enable features for sharing",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        if (needsNfc) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NFC is off — needed for Tap to Share",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Turn on NFC")
                }
            }
        }
        if (needsWifi) {
            if (needsNfc) Spacer(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wi‑Fi is off — needed for nearby device discovery",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Turn on Wi‑Fi")
                }
            }
        }
    }
}
