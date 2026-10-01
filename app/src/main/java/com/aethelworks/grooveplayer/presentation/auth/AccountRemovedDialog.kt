package com.aethelworks.grooveplayer.presentation.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.aethelworks.grooveplayer.domain.auth.AccountRemovedNotice
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun AccountRemovedDialog(
    onDismiss: () -> Unit,
) {
    val colors = GrooveTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = SoftWhite,
        title = { Text("Signed out") },
        text = { Text(AccountRemovedNotice.MESSAGE) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = colors.accent)
            }
        },
    )
}
