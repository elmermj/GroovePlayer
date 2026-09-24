package com.aethelsoft.grooveplayer.presentation.library.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.presentation.common.GrooveActionButton
import com.aethelsoft.grooveplayer.presentation.common.GrooveBelowAppBarSpacer
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.library.ui.ItemSelectionConfig
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.GrooveTheme

@Composable
fun AddPlaylistTracksScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPlaylistTracksViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsState()
    val query by viewModel.query.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    GrooveScreen(
        title = "Add tracks",
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues(
            start = M_PADDING,
            end = M_PADDING,
            bottom = M_PADDING + grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer()),
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GrooveBelowAppBarSpacer()
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search songs") },
            )
            when {
                isLoading && songs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GrooveTheme.colors.onSurface)
                    }
                }
                songs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GrooveMutedText(
                            if (query.isBlank()) "No songs in your library" else "No matching songs",
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = M_PADDING),
                        verticalArrangement = Arrangement.spacedBy(XS_PADDING),
                    ) {
                        items(songs, key = { it.id }) { song ->
                            SongItemComponent(
                                song = song,
                                onClick = {},
                                selectionConfig = ItemSelectionConfig(
                                    isSelected = song.id in selectedIds,
                                    onSelectedChange = { selected ->
                                        selectedIds = if (selected) {
                                            selectedIds + song.id
                                        } else {
                                            selectedIds - song.id
                                        }
                                    },
                                ),
                            )
                        }
                    }
                    GrooveActionButton(
                        label = if (selectedIds.isEmpty()) {
                            "Select tracks"
                        } else {
                            "Add ${selectedIds.size}"
                        },
                        onClick = { viewModel.add(selectedIds, onNavigateBack) },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
