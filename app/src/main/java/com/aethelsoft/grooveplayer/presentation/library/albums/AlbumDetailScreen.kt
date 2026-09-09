package com.aethelsoft.grooveplayer.presentation.library.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.parseAlbumId
import com.aethelsoft.grooveplayer.presentation.common.GrooveMutedText
import com.aethelsoft.grooveplayer.presentation.common.GrooveScreen
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.XS_PADDING
import com.aethelsoft.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun AlbumDetailScreen(
    albumId: String,
    onNavigateBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(albumId) {
        viewModel.load(albumId)
    }

    val songsPagingItems: LazyPagingItems<Song> = viewModel.songsPagingFlow.collectAsLazyPagingItems()
    val playerViewModel = rememberPlayerViewModel()
    val (artistName, albumName) = parseAlbumId(albumId)
    val titleText = if (artistName.isNotEmpty()) {
        "$albumName by $artistName"
    } else {
        albumName
    }

    GrooveScreen(
        title = titleText,
        onBackClick = onNavigateBack,
        contentPadding = PaddingValues.Zero,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = M_PADDING, vertical = M_PADDING),
            verticalArrangement = Arrangement.spacedBy(XS_PADDING),
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
                        }
                    )
                }
            }
            songsPagingItems.loadState.apply {
                when {
                    refresh is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = SoftWhite)
                            }
                        }
                    }
                    append is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(M_PADDING),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = SoftWhite)
                            }
                        }
                    }
                    refresh is LoadState.NotLoading && songsPagingItems.itemCount == 0 -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                GrooveMutedText("No songs in this album")
                            }
                        }
                    }
                }
            }
        }
    }
}
