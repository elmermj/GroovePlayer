package com.aethelsoft.grooveplayer.presentation.profile.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.presentation.profile.ui.ActionType
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileRowIcon
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSectionComponent
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingRow
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileStorageSection
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XAppVersion
import com.aethelsoft.grooveplayer.utils.theme.icons.XCopyright
import com.aethelsoft.grooveplayer.utils.theme.icons.XPrivacyPolicy
import com.aethelsoft.grooveplayer.utils.theme.icons.XRecentUpdates
import com.aethelsoft.grooveplayer.utils.theme.icons.XUiStyle
import com.aethelsoft.grooveplayer.utils.theme.icons.XShareMusic
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun TabletProfileLayout(
    viewModel: ProfileViewModel,
    onNavigateToShare: () -> Unit = {},
    onNavigateToUiStyling: () -> Unit = {},
) {
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
                    .width(360.dp)
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