package com.aethelsoft.grooveplayer.presentation.library.songs.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.ui.ItemSelectionConfig
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.S_PADDING

@Composable
fun PhoneSongsLayout(
    songsPagingItems: LazyPagingItems<Song>,
    paddingValues: PaddingValues,
    horizontalPadding: Dp,
    onEditSong: (Song) -> Unit = {},
    onLongPress: (Song) -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    bottomPaddingForSelectionBar: Dp = 0.dp
) {
    val playerViewModel = rememberPlayerViewModel()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 16.dp,
            bottom = 16.dp + bottomPaddingForSelectionBar
        ),
        verticalArrangement = Arrangement.spacedBy(S_PADDING / 2)
    ) {
        items(
            count = songsPagingItems.itemCount,
            key = songsPagingItems.itemKey { it.id }
        ) { index ->
            val song = songsPagingItems[index]
            if (song != null) {
                val list = songsPagingItems.itemSnapshotList.mapNotNull { it }
                SongItemComponent(
                    song = song,
                    onClick = {
                        if (index < list.size) {
                            playerViewModel.setQueue(list, index)
                        }
                    },
                    onEditMetadata = { onEditSong(it) },
                    onLongPress = onLongPress,
                    padding = 0.dp,
                    selectionConfig = if (isSelectionMode) {
                        ItemSelectionConfig(
                            isSelected = song.id in selectedIds,
                            onSelectedChange = { onToggleSelection(song.id) }
                        )
                    } else null
                )
            }
        }
        songsPagingItems.loadState.apply {
            when {
                refresh is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                append is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                refresh is LoadState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error: ${(refresh as LoadState.Error).error.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(onClick = { songsPagingItems.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}