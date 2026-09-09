package com.aethelsoft.grooveplayer.presentation.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.rememberNavigationActions
import com.aethelsoft.grooveplayer.utils.theme.icons.XNFC
import com.aethelsoft.grooveplayer.utils.theme.icons.XShare
import com.aethelsoft.grooveplayer.utils.theme.icons.XWifiSync
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

/**
 * Player chrome share control: opens NFC / nearby share for the current [song].
 * Place next to the Bluetooth button in the player app bar.
 */
@Composable
fun PlayerShareButton(
    song: Song?,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
) {
    val navigation = rememberNavigationActions()
    var expanded by remember { mutableStateOf(false) }
    val enabled = song != null
    val colors = GrooveTheme.colors

    Box(modifier = modifier) {
        IconButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
        ) {
            Icon(
                imageVector = XShare,
                contentDescription = "Share song",
                tint = if (enabled) Color.Unspecified else iconTint.copy(alpha = 0.35f),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Tap to share",
                        style = GrooveTheme.typography.buttonLabel.toTextStyle(),
                        color = colors.onSurface,
                    )
                },
                leadingIcon = {
                    Icon(XNFC, contentDescription = null, tint = colors.onSurface)
                },
                onClick = {
                    expanded = false
                    song?.let { navigation.openShareViaNfcWithSongs(listOf(it)) }
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Share with nearby device",
                        style = GrooveTheme.typography.buttonLabel.toTextStyle(),
                        color = colors.onSurface,
                    )
                },
                leadingIcon = {
                    Icon(XWifiSync, contentDescription = null, tint = colors.onSurface)
                },
                onClick = {
                    expanded = false
                    song?.let { navigation.openShareViaNearbyWithSongs(listOf(it)) }
                },
            )
        }
    }
}
