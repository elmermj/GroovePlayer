package com.aethelsoft.grooveplayer.presentation.profile.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.profile.ProfileViewModel
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSectionComponent
import com.aethelsoft.grooveplayer.presentation.profile.ui.ProfileSettingRow
import com.aethelsoft.grooveplayer.presentation.profile.ui.ActionType
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING

@Composable
fun PhoneProfileLayout(
    viewModel: ProfileViewModel,
    onNavigateToShare: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = M_PADDING)
    ) {
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .background(Color.Black)
                    .height(
                        APP_BAR_HEIGHT + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + M_PADDING
                    )
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
                ProfileSettingRow(
                    title = "Share Music",
                    subtitle = "Share via Tap (NFC) or nearby device",
                    actionType = ActionType.EXPANDABLE,
                    onClick = onNavigateToShare
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
             * - A button for consolidating specific folders which contain music files into a specific folder.
             * - Clear cache.
             */
            ProfileSectionComponent(
                sectionTitle = "Storage",
            ) {
                val storageActiveRowId by viewModel.storageActiveRowId.collectAsState()
                ExcludedFoldersRow(
                    viewModel = viewModel,
                    isExpanded = storageActiveRowId == "excluded_folders",
                    onExpandedChange = { expanded ->
                        viewModel.setStorageActiveRowId(if (expanded) "excluded_folders" else null)
                    }
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Storage usage",
                    subtitle = "View how much space GroovePlayer uses"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Consolidate music folders",
                    subtitle = "Move scattered music into a single location"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Clear cache",
                    subtitle = "Remove temporary data"
                )
            }
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
                    title = "App version",
                    subtitle = "See current version"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Recent updates",
                    subtitle = "What’s new in GroovePlayer"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Copyright & licenses",
                    subtitle = "Legal information"
                )
                Spacer(Modifier.height(S_PADDING))
                ProfileSettingRow(
                    title = "Privacy policy",
                    subtitle = "How your data is handled"
                )
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    .height(M_PADDING + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 240.dp)
                    .width(420.dp)
            )
        }
    }
}
