package com.aethelsoft.grooveplayer.presentation.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.aethelsoft.grooveplayer.domain.backup.LoginRestorePrompt
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun LoginRestorePromptDialog(
    onKeepCurrent: () -> Unit,
    onRestore: () -> Unit,
) {
    val colors = GrooveTheme.colors
    AlertDialog(
        onDismissRequest = onKeepCurrent,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = SoftWhite,
        title = { Text(LoginRestorePrompt.TITLE) },
        text = {
            Text(
                "A cloud library backup is on your account. " +
                    "No leaves the library on this device unchanged. " +
                    "Yes downloads that backup, checks the file, and applies it.",
            )
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onKeepCurrent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = LoginRestorePrompt.KEEP_CURRENT,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = colors.muted.copy(alpha = 0.75f),
                    )
                }
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = LoginRestorePrompt.RESTORE,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = colors.accent,
                    )
                }
            }
        },
    )
}
