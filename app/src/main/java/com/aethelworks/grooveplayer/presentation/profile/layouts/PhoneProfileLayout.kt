package com.aethelworks.grooveplayer.presentation.profile.layouts

import androidx.compose.ui.res.stringResource
import com.aethelworks.grooveplayer.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.presentation.common.topBarContentInset
import com.aethelworks.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileRowIcon
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileSectionComponent
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileSettingRow
import com.aethelworks.grooveplayer.presentation.profile.ui.ProfileStorageSection
import com.aethelworks.grooveplayer.presentation.profile.ui.ActionType
import com.aethelworks.grooveplayer.utils.M_PADDING
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.icons.XAppVersion
import com.aethelworks.grooveplayer.utils.theme.icons.XCopyright
import com.aethelworks.grooveplayer.utils.LegalUrls
import com.aethelworks.grooveplayer.utils.theme.icons.XPrivacyPolicy
import com.aethelworks.grooveplayer.utils.theme.icons.XRecentUpdates
import com.aethelworks.grooveplayer.utils.theme.icons.XUiStyle
import com.aethelworks.grooveplayer.utils.theme.icons.XShareMusic
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun PhoneProfileLayout(
    viewModel: ProfileViewModel,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onOpenRecentUpdates: () -> Unit = {},
) {
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

                // Cross-fade UI hidden until real overlapping crossfade ships (fadeTimer path is stubbed).
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
                sectionTitle = stringResource(R.string.settings_about),
            ) {
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XAppVersion) },
                    title = stringResource(R.string.settings_app_version),
                    subtitle = stringResource(R.string.settings_app_version_sub)
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
                    actionType = ActionType.LINK,
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_sub),
                    onClick = { LegalUrls.open(context, LegalUrls.PRIVACY) },
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    icon = { ProfileRowIcon(XCopyright) },
                    actionType = ActionType.LINK,
                    title = stringResource(R.string.settings_terms),
                    subtitle = stringResource(R.string.settings_terms_sub),
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
