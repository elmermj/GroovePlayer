package com.aethelsoft.grooveplayer.presentation.ui_customisation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun CustomisationConfirmDialog(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val colors = GrooveTheme.colors
    AlertDialog(
        onDismissRequest = { (onCancel ?: onDismiss)() },
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.muted.copy(alpha = 0.85f),
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = colors.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = colors.muted.copy(alpha = 0.75f))
            }
        },
    )
}