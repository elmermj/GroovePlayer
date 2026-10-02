package com.aethelworks.grooveplayer.presentation.login_restore

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

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
        title = { Text(stringResource(R.string.login_restore_title)) },
        text = {
            Text(stringResource(R.string.login_restore_body))
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onKeepCurrent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.login_restore_keep),
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
                        text = stringResource(R.string.login_restore_yes),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = colors.accent,
                    )
                }
            }
        },
    )
}
