package com.aethelworks.grooveplayer.presentation.profile.layouts

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.domain.model.RepeatMode
import com.aethelworks.grooveplayer.domain.model.VisualizationMode
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelworks.grooveplayer.presentation.equalizer.ui.EqualizerControlsComponent
import com.aethelworks.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelworks.grooveplayer.presentation.profile.ui.ActionType
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileRowIcon
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileSectionComponent
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileSettingRow
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileSettingsButton
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileStorageSection
import com.aethelworks.grooveplayer.utils.LegalUrls
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.rememberNotificationPermissionState
import com.aethelworks.grooveplayer.utils.theme.icons.XAppVersion
import com.aethelworks.grooveplayer.utils.theme.icons.XCopyright
import com.aethelworks.grooveplayer.utils.theme.icons.XCrossFade
import com.aethelworks.grooveplayer.utils.theme.icons.XEqualizer
import com.aethelworks.grooveplayer.utils.theme.icons.XMiniPlayer
import com.aethelworks.grooveplayer.utils.theme.icons.XNotifications
import com.aethelworks.grooveplayer.utils.theme.icons.XPrivacyPolicy
import com.aethelworks.grooveplayer.utils.theme.icons.XRecentUpdates
import com.aethelworks.grooveplayer.utils.theme.icons.XRepeatMode
import com.aethelworks.grooveplayer.utils.theme.icons.XUiStyle
import com.aethelworks.grooveplayer.utils.theme.icons.XShareMusic
import com.aethelworks.grooveplayer.utils.theme.icons.XShuffleMode
import com.aethelworks.grooveplayer.utils.theme.icons.XVisualization
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.aethelworks.grooveplayer.BuildConfig

@Composable
fun LargeTabletProfileLayout(
    viewModel: ProfileViewModel,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onOpenRecentUpdates: () -> Unit = {},
){
    val context = LocalContext.current
    val canvas = GrooveTheme.colors.canvas
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(canvas)
            .padding(horizontal = M_PADDING)
    ) {
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .background(canvas)
                    .height(topBarContentInset())
            )
        }
        item {
            /** xdev
             * Account settings
             *
             * - User account (free, basic, premium) with basic and premium requires Google OAuth login
             * - Reset account
             */
            ProfileSectionComponent(
                sectionTitle = stringResource(R.string.settings_account),
            ) {
                val activeRowId by viewModel.activeRowId.collectAsState()

                ProfileSettingRow(
                    icon = { ProfileRowIcon(XShareMusic) },
                    title = stringResource(R.string.settings_share_music),
                    subtitle = stringResource(R.string.settings_share_music_sub),
                    actionType = ActionType.EXPANDABLE,
                    onClick = onNavigateToShare
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XUiStyle) },
                    title = stringResource(R.string.settings_ui_customisation),
                    subtitle = stringResource(R.string.settings_ui_customisation_sub),
                    actionType = ActionType.LINK,
                    onClick = onNavigateToUiStyling,
                )
                Spacer(Modifier.height(S_PADDING))
                NotificationsRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "notifications",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "notifications" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))
                AccountSection(viewModel = viewModel, onNavigateToBackup = onNavigateToBackup)
                Spacer(Modifier.height(S_PADDING))
                // FREE-tier only; no-op when Basic/Premium.
                com.aethelworks.grooveplayer.presentation.ads.BannerAdSlot()
            }
        }

        item {
            /** xdev
             * Playback settings
             *
             * - Repeat, shuffle, fade modes.
             * - Turn on/off MiniPlayerBar at start.
             * - Default mode for visualization.
             * - Equalizer preset and settings.
             */
            ProfileSectionComponent(
                sectionTitle = stringResource(R.string.settings_playback),
            ) {
                val activeRowId by viewModel.activeRowId.collectAsState()

                RepeatModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "repeat",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "repeat" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                ShuffleModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "shuffle",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "shuffle" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                // Cross-fade UI hidden until real crossfade is implemented (P1).
                // CrossFadeModeRow(... fade ...)
                // Spacer(Modifier.height(S_PADDING))
MiniPlayerOnStartRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "mini_player",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "mini_player" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                VisualizationModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "visualization",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "visualization" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

                EqualizerRow(
                    isExpanded = activeRowId == "equalizer",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "equalizer" else null)
                    }
                )
            }
        }

        item {
            /** xdev
             * Storage settings
             *
             * - Excluded folders.
             * - Storage usage.
             * - Consolidate music folders.
             * - Clear cache.
             */
            ProfileStorageSection(viewModel = viewModel)
        }
        item {
            /** xdev
             * About section
             *
             * - App version.
             * - App recent updates.
             * - App copyright.
             * - App privacy policy.
             */
            ProfileSectionComponent(
                sectionTitle = stringResource(R.string.settings_about),
            ) {
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XAppVersion) },
                    title = stringResource(R.string.settings_app_version),
                    subtitle = stringResource(R.string.settings_app_version_value, BuildConfig.VERSION_NAME)
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XRecentUpdates) },
                    title = stringResource(R.string.settings_recent_updates),
                    subtitle = stringResource(R.string.settings_recent_updates_sub),
                    actionType = ActionType.LINK,
                    onClick = onOpenRecentUpdates,
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XCopyright) },
                    title = stringResource(R.string.settings_copyright),
                    subtitle = stringResource(R.string.settings_legal)
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XPrivacyPolicy) },
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_sub_we),
                    actionType = ActionType.LINK,
                    onClick = { LegalUrls.open(context, LegalUrls.PRIVACY) },
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XPrivacyPolicy) },
                    title = stringResource(R.string.settings_terms),
                    subtitle = stringResource(R.string.settings_terms_sub),
                    actionType = ActionType.LINK,
                    onClick = { LegalUrls.open(context, LegalUrls.TERMS) },
                )
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    .height(M_PADDING + grooveBottomContentInset(includeMiniPlayer = true) + 134.dp)
                    .width(420.dp)
            )
        }
    }
}

/**
 * Shared by Phone, Tablet, and LargeTablet. Sign-out and delete both confirm
 * inside [com.aethelworks.grooveplayer.presentation.profile.ui.AccountAuthHeader].
 */
@Composable
fun AccountSection(
    viewModel: ProfileViewModel,
    onNavigateToBackup: () -> Unit = {},
) {
    com.aethelworks.grooveplayer.presentation.profile.ui.AccountAuthHeader(viewModel = viewModel)
    com.aethelworks.grooveplayer.presentation.profile.ui.SubscriptionPaywallSection(
        onNavigateToBackup = onNavigateToBackup,
    )
}

@Composable
fun RepeatModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val playerViewModel = rememberPlayerViewModel()
    val currentRepeat by playerViewModel.repeat.collectAsState()

    ProfileSettingRow(
        icon = { ProfileRowIcon(XRepeatMode) },
        actionType = ActionType.EXPANDABLE,
        title = stringResource(R.string.settings_repeat),
        subtitle = stringResource(R.string.settings_repeat_sub),
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val modes = listOf(
                RepeatMode.OFF to stringResource(R.string.action_off),
                RepeatMode.ALL to stringResource(R.string.action_all),
                RepeatMode.ONE to stringResource(R.string.action_one)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                modes.forEach { (mode, label) ->
                    val isActive = mode == currentRepeat
                    ProfileSettingsButton(
                        onClick = {
                            playerViewModel.setRepeat(mode)
                            onExpandedChange(false)
                        },
                        modifier = Modifier
                            .weight(1f),
                        title = label,
                        isActive = isActive
                    )
                }
            }
        }
    )
}

@Composable
fun ShuffleModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val playerViewModel = rememberPlayerViewModel()
    val isEnabled by playerViewModel.shuffle.collectAsState()

    ProfileSettingRow(
        icon = { ProfileRowIcon(XShuffleMode) },
        actionType = ActionType.EXPANDABLE,
        title = stringResource(R.string.settings_shuffle),
        subtitle = stringResource(R.string.settings_shuffle_sub),
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                ProfileSettingsButton(
                    onClick = {
                        playerViewModel.setShuffle(false)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.action_off),
                    isActive = !isEnabled
                )
                ProfileSettingsButton(
                    onClick = {
                        playerViewModel.setShuffle(true)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.action_on),
                    isActive = isEnabled
                )
            }
        }
    )
}

@Composable
fun CrossFadeModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val settings by viewModel.userSettings.collectAsState()
    val fadeSeconds = settings.fadeTimer.coerceIn(0, 10)
    var sliderValue = fadeSeconds.toFloat()

    ProfileSettingRow(
        icon = { ProfileRowIcon(XCrossFade) },
        actionType = ActionType.EXPANDABLE,
        title = stringResource(R.string.settings_crossfade),
        subtitle = stringResource(R.string.settings_crossfade_sub),
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomSlider(
                    value = sliderValue,
                    onValueChange = { value ->
                        sliderValue = value
                    },
                    onValueChangeFinished = {
                        viewModel.setFadeTimer(sliderValue.toInt())
                    },
                    valueRange = 0f..10f,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.3f)
                )
                Text(
                    text = stringResource(R.string.settings_crossfade_seconds, sliderValue.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite
                )
            }
        }
    )
}

@Composable
fun MiniPlayerOnStartRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val isEnabled by viewModel.isMiniPlayerOnStartEnabled.collectAsState()

    ProfileSettingRow(
        icon = { ProfileRowIcon(XMiniPlayer) },
        title = stringResource(R.string.settings_mini_player),
        subtitle = stringResource(R.string.settings_mini_player_sub),
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setMiniPlayerOnStartEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.6f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    )
}

@Composable
fun NotificationsRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val preferenceEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val (hasPermission, requestPermission) = rememberNotificationPermissionState()
    val context = LocalContext.current
    val isEffectivelyEnabled = preferenceEnabled && hasPermission
    val statusText = when {
        isEffectivelyEnabled -> stringResource(R.string.settings_enabled)
        preferenceEnabled && !hasPermission -> stringResource(R.string.settings_permission_required)
        else -> stringResource(R.string.settings_disabled)
    }

    ProfileSettingRow(
        icon = { ProfileRowIcon(XNotifications) },
        title = stringResource(R.string.settings_notifications),
        subtitle = stringResource(R.string.settings_notifications_sub, statusText),
        actionType = ActionType.EXPANDABLE,
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftWhite
                    )
                    Switch(
                        checked = isEffectivelyEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                viewModel.setNotificationsEnabled(true)
                                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermission()
                                }
                            } else {
                                viewModel.setNotificationsEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.6f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
                if (preferenceEnabled && !hasPermission) {
                    ProfileSettingsButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.settings_open_system),
                        isActive = false,
                    )
                }
            }
        }
    )
}

@Composable
fun VisualizationModeRow(
    viewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val settings by viewModel.userSettings.collectAsState()
    val mode = settings.visualizationMode

    ProfileSettingRow(
        icon = { ProfileRowIcon(XVisualization) },
        actionType = ActionType.EXPANDABLE,
        title = stringResource(R.string.settings_visualization),
        subtitle = stringResource(R.string.settings_visualization_sub),
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val options = listOf(
                VisualizationMode.OFF to stringResource(R.string.action_off),
                VisualizationMode.SIMULATED to stringResource(R.string.settings_simulated),
                VisualizationMode.REAL_TIME to stringResource(R.string.settings_dynamic)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(S_PADDING)
            ) {
                options.forEach { (value, label) ->
                    val isActive = value == mode
                    ProfileSettingsButton(
                        onClick = {
                            viewModel.setVisualizationMode(value)
                            onExpandedChange(false)
                        },
                        modifier = Modifier.weight(1f, fill = false),
                        title = label,
                        isActive = isActive
                    )
                }
            }
        }
    )
}

@Composable
fun EqualizerRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    ProfileSettingRow(
        icon = { ProfileRowIcon(XEqualizer) },
        actionType = ActionType.EXPANDABLE,
        title = stringResource(R.string.settings_equalizer),
        subtitle = stringResource(R.string.settings_equalizer_sub),
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val consumeScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset = available
                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity = available
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1200.dp)
                    .nestedScroll(consumeScrollConnection)
            ) {
                EqualizerControlsComponent(
                    modifier = Modifier
                        .padding(0.dp)
                        .fillMaxWidth(),
                    isSimplified = true
                )
            }
        }
    )
}