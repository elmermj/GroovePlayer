package com.aethelsoft.grooveplayer.presentation.profile.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.domain.model.RepeatMode
import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.player.ui.CustomSlider
import com.aethelsoft.grooveplayer.presentation.player.ui.EqualizerControlsComponent
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.presentation.profile.ui.ActionType
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileRowIcon
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSectionComponent
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingRow
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingsButton
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileStorageSection
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.rememberNotificationPermissionState
import com.aethelsoft.grooveplayer.utils.theme.icons.XAccountType
import com.aethelsoft.grooveplayer.utils.theme.icons.XAppVersion
import com.aethelsoft.grooveplayer.utils.theme.icons.XCopyright
import com.aethelsoft.grooveplayer.utils.theme.icons.XCrossFade
import com.aethelsoft.grooveplayer.utils.theme.icons.XEqualizer
import com.aethelsoft.grooveplayer.utils.theme.icons.XMiniPlayer
import com.aethelsoft.grooveplayer.utils.theme.icons.XNotifications
import com.aethelsoft.grooveplayer.utils.theme.icons.XPrivacyPolicy
import com.aethelsoft.grooveplayer.utils.theme.icons.XRecentUpdates
import com.aethelsoft.grooveplayer.utils.theme.icons.XRepeatMode
import com.aethelsoft.grooveplayer.utils.theme.icons.XUiStyle
import com.aethelsoft.grooveplayer.utils.theme.icons.XShareMusic
import com.aethelsoft.grooveplayer.utils.theme.icons.XShuffleMode
import com.aethelsoft.grooveplayer.utils.theme.icons.XVisualization
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext

@Composable
fun LargeTabletProfileLayout(
    viewModel: ProfileViewModel,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
){
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
                sectionTitle = "Account",
            ) {
                val activeRowId by viewModel.activeRowId.collectAsState()

                ProfileSettingRow(
                    icon = { ProfileRowIcon(XShareMusic) },
                    title = "Share Music",
                    subtitle = "Share via Tap (NFC) or nearby device",
                    actionType = ActionType.EXPANDABLE,
                    onClick = onNavigateToShare
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XUiStyle) },
                    title = "UI Customisation",
                    subtitle = "Customize colors, type, spacing, and more",
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
                AccountSection(viewModel = viewModel)
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
                sectionTitle = "Playback",
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

                CrossFadeModeRow(
                    viewModel = viewModel,
                    isExpanded = activeRowId == "fade",
                    onExpandedChange = { expanded ->
                        viewModel.setActiveRowId(if (expanded) "fade" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))

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
                sectionTitle = "About",
            ) {
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XAppVersion) },
                    title = "App version",
                    subtitle = "See current version"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XRecentUpdates) },
                    title = "Recent updates",
                    subtitle = "What’s new in GroovePlayer"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XCopyright) },
                    title = "Copyright & licenses",
                    subtitle = "Legal information"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XPrivacyPolicy) },
                    title = "Privacy policy",
                    subtitle = "How your data is handled"
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

@Composable
fun AccountSection(viewModel: ProfileViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val tier = profile?.privilegeTier ?: com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.FREE

    ProfileSettingRow(
        icon = { ProfileRowIcon(XAccountType) },
        title = "Account type",
        subtitle = when (tier) {
            com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.FREE -> "Free"
            com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.BASIC -> "Basic"
            com.aethelsoft.grooveplayer.domain.model.PrivilegeTier.PREMIUM -> "Premium"
        }
    )
    Spacer(Modifier.height(S_PADDING))
    ProfileSettingsButton(
        onClick = {
            // Placeholder: in future replace with real Google OAuth.
            // For now, simply indicate that sign-in is not implemented.
        },
        modifier = Modifier.fillMaxWidth(),
        title = "Sign in with Google",
        isActive = false,
    )
    Spacer(Modifier.height(S_PADDING))
    ProfileSettingsButton(
        onClick = {
            // TODO: wire to a real "reset account" flow when available.
        },
        modifier = Modifier.fillMaxWidth(),
        title = "Reset account",
        isActive = false,
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
        title = "Repeat mode",
        subtitle = "Toggle repeat mode",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val modes = listOf(
                RepeatMode.OFF to "Off",
                RepeatMode.ALL to "All",
                RepeatMode.ONE to "One"
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
        title = "Shuffle mode",
        subtitle = "Toggle shuffle mode",
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
                    title = "Off",
                    isActive = !isEnabled
                )
                ProfileSettingsButton(
                    onClick = {
                        playerViewModel.setShuffle(true)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.weight(1f),
                    title = "On",
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
        title = "Cross-fade mode",
        subtitle = "Enable and set duration for smooth transitions between songs",
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
                    text = "${sliderValue.toInt()} s",
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
        title = "Show mini player on app start",
        subtitle = "Toggle mini player visibility at launch",
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
                    text = if (isEnabled) "Enabled" else "Disabled",
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
        isEffectivelyEnabled -> "Enabled"
        preferenceEnabled && !hasPermission -> "Permission required"
        else -> "Disabled"
    }

    ProfileSettingRow(
        icon = { ProfileRowIcon(XNotifications) },
        title = "Notifications",
        subtitle = "Playback and transfer alerts · $statusText",
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
                        title = "Open system settings",
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
        title = "Default visualization mode",
        subtitle = "Choose how the visualizer looks by default",
        isSecondaryVisible = isExpanded,
        onSecondaryVisibleChange = onExpandedChange,
        secondaryContent = {
            val options = listOf(
                VisualizationMode.OFF to "Off",
                VisualizationMode.SIMULATED to "Simulated",
                VisualizationMode.REAL_TIME to "Real-time"
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
        title = "Equalizer",
        subtitle = "Preset & advanced settings",
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