package com.aethelsoft.grooveplayer.presentation.plan

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.domain.plan.PlanExpiry
import com.aethelsoft.grooveplayer.utils.theme.ui.GroovePlayerTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun PlanExpiryDialog(
    message: String,
    onManagePlan: () -> Unit,
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
        title = { Text(message) },
        text = { Text("You can renew or change the plan from your account page.") },
        confirmButton = {
            TextButton(onClick = onManagePlan) {
                Text(PlanExpiry.MANAGE, color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(PlanExpiry.DISMISS, color = colors.muted.copy(alpha = 0.75f))
            }
        },
    )
}

@Preview(showBackground = true, name = "Plan expiring soon")
@Composable
private fun PlanExpiryExpiringSoonPreview() {
    GroovePlayerTheme {
        PlanExpiryDialog(
            message = PlanExpiry.message(
                PlanExpiry.State.EXPIRING_SOON,
                PrivilegeTier.PREMIUM,
                days = 2,
            ),
            onManagePlan = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, name = "Plan in grace")
@Composable
private fun PlanExpiryInGracePreview() {
    GroovePlayerTheme {
        PlanExpiryDialog(
            message = PlanExpiry.message(
                PlanExpiry.State.IN_GRACE,
                PrivilegeTier.PREMIUM,
                days = 0,
            ),
            onManagePlan = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, name = "Plan expired")
@Composable
private fun PlanExpiryExpiredPreview() {
    GroovePlayerTheme {
        PlanExpiryDialog(
            message = PlanExpiry.message(
                PlanExpiry.State.EXPIRED,
                PrivilegeTier.BASIC,
                days = 0,
            ),
            onManagePlan = {},
            onDismiss = {},
        )
    }
}
