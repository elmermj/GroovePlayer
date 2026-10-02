package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
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
import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import com.aethelworks.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.icons.XAccountType
import com.aethelworks.grooveplayer.utils.theme.icons.XUser
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

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
    val serverRetryInFlight by viewModel.serverRetryInFlight.collectAsState()
    val openRestoreInFlight by viewModel.openRestoreInFlight.collectAsState()
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
        PrivilegeTier.FREE -> stringResource(R.string.tier_free)
        PrivilegeTier.BASIC -> stringResource(R.string.tier_basic)
        PrivilegeTier.PREMIUM -> stringResource(R.string.tier_premium)
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
                    contentDescription = stringResource(R.string.cd_profile_avatar),
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
                    text = displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.auth_signed_in),
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
                    text = stringResource(R.string.auth_tier_line, tierLabel),
                    style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                    color = SoftWhite,
                )
            }
        }
        Spacer(Modifier.height(S_PADDING))
    }

    ProfileSettingRow(
        icon = { ProfileRowIcon(XAccountType) },
        title = stringResource(R.string.account_type),
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
            title = stringResource(R.string.sign_out),
            isActive = true,
            isInverse = true,
        )
        Spacer(Modifier.height(S_PADDING))
        ProfileSettingsButton(
            onClick = { showDeleteAccountConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.auth_delete_account),
            isActive = true,
            isInverse = true,
        )
    } else {
        ProfileSettingsButton(
            onClick = {
                if (activity != null) viewModel.signInWithGoogle(activity)
            },
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.sign_in_with_google),
            isActive = activity != null,
        )
    }

    val syncBanner = authError ?: serverSyncError
    val canRetryServer = signedIn && !serverSyncError.isNullOrBlank()
    if (!syncBanner.isNullOrBlank()) {
        Spacer(Modifier.height(S_PADDING))
        Text(
            text = syncBanner,
            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
            color = Color(0xFFFF8A80),
        )
    }
    if (canRetryServer) {
        Spacer(Modifier.height(S_PADDING))
        ProfileSettingsButton(
            onClick = { viewModel.retryServerSync() },
            modifier = Modifier.fillMaxWidth(),
            title = if (serverRetryInFlight) stringResource(R.string.action_retrying) else stringResource(R.string.action_retry),
            isActive = !serverRetryInFlight && !openRestoreInFlight && !authLoading,
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
        title = { Text(stringResource(R.string.auth_sign_out_title)) },
        text = {
            Text(stringResource(R.string.auth_sign_out_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.sign_out), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.muted.copy(alpha = 0.75f))
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
        title = { Text(stringResource(R.string.auth_delete_title)) },
        text = {
            Text(stringResource(R.string.auth_delete_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.auth_delete_account), color = Color(0xFFFF8A80))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.muted.copy(alpha = 0.75f))
            }
        },
    )
}
