package com.aethelsoft.grooveplayer.presentation.profile.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.domain.model.PrivilegeTier
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XAccountType
import com.aethelsoft.grooveplayer.utils.theme.icons.XUser
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

/**
 * Sign-In / account header for Profile (phone, tablet, large tablet).
 * Shows name, avatar, and tier label. Premium is labeled but never upsold.
 */
@Composable
fun AccountAuthHeader(viewModel: ProfileViewModel) {
    val authUser by viewModel.authUser.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val serverSyncError by viewModel.serverSyncError.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val signedIn = authUser != null
    // When signed out, never fall back to a stale Room profile for identity/tier.
    val displayName = if (signedIn) {
        authUser?.displayName ?: profile?.username
    } else null
    val email = if (signedIn) (authUser?.email ?: profile?.email) else null
    val avatar = if (signedIn) (authUser?.avatarUrl ?: profile?.profilePictureUrl) else null
    val tier = if (signedIn) {
        authUser?.privilegeTier ?: profile?.privilegeTier ?: PrivilegeTier.FREE
    } else {
        PrivilegeTier.FREE
    }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    // Tier label: Free / Basic. Premium shown as "Premium" without upsell chrome.
    val tierLabel = when (tier) {
        PrivilegeTier.FREE -> "Free"
        PrivilegeTier.BASIC -> "Basic"
        PrivilegeTier.PREMIUM -> "Premium"
    }

    if (signedIn) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!avatar.isNullOrBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = "Profile avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                )
            } else {
                ProfileRowIcon(XUser)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName?.takeIf { it.isNotBlank() } ?: "Signed in",
                    style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
                    color = GrooveTheme.colors.onSurface,
                )
                if (!email.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                        color = SoftWhite,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tier · $tierLabel",
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                    color = SoftWhite,
                )
            }
        }
        Spacer(Modifier.height(S_PADDING))
    }

    ProfileSettingRow(
        icon = { ProfileRowIcon(XAccountType) },
        title = "Account type",
        subtitle = tierLabel,
    )
    Spacer(Modifier.height(S_PADDING))

    val context = LocalContext.current
    val activity = context as? Activity

    if (authLoading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = GrooveTheme.colors.accent,
                strokeWidth = 2.dp,
            )
        }
        Spacer(Modifier.height(S_PADDING))
    } else if (signedIn) {
        ProfileSettingsButton(
            onClick = { showSignOutConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            title = "Sign out",
            isActive = true,
            isInverse = true,
        )
        Spacer(Modifier.height(S_PADDING))
        ProfileSettingsButton(
            onClick = { showDeleteAccountConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            title = "Delete account",
            isActive = true,
            isInverse = true,
        )
    } else {
        ProfileSettingsButton(
            onClick = {
                if (activity != null) viewModel.signInWithGoogle(activity)
            },
            modifier = Modifier.fillMaxWidth(),
            title = "Sign in with Google",
            isActive = activity != null,
        )
    }

    val syncBanner = authError ?: serverSyncError
    if (!syncBanner.isNullOrBlank()) {
        Spacer(Modifier.height(S_PADDING))
        Text(
            text = syncBanner,
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFF8A80),
        )
    }
    if (showSignOutConfirm) {
        SignOutConfirmDialog(
            onConfirm = {
                showSignOutConfirm = false
                viewModel.signOut()
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }
    if (showDeleteAccountConfirm) {
        DeleteAccountConfirmDialog(
            onConfirm = {
                showDeleteAccountConfirm = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteAccountConfirm = false },
        )
    }
}

@Composable
private fun SignOutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GrooveTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = SoftWhite,
        title = { Text("Sign out?") },
        text = {
            Text(
                "This device returns to Free with ads on. Cloud backup and Premium stay on " +
                    "your account until you sign in again. The next Sign in with Google " +
                    "will ask which account to use.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sign out", color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.muted.copy(alpha = 0.75f))
            }
        },
    )
}

@Composable
private fun DeleteAccountConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GrooveTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = SoftWhite,
        title = { Text("Delete account?") },
        text = {
            Text(
                "This permanently deletes your GroovePlayer account, cloud backups, and " +
                    "signed-in sessions on every device. It cannot be undone.\n\n" +
                    "If you have an active Play subscription or +20GB add-on, cancel it in " +
                    "Google Play first (Play Store → Payments & subscriptions). Deleting the " +
                    "account does not cancel Play Billing — charges can continue until you cancel there.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete account", color = Color(0xFFFF8A80))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.muted.copy(alpha = 0.75f))
            }
        },
    )
}
