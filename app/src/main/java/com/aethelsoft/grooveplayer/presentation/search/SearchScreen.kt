package com.aethelsoft.grooveplayer.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.aethelsoft.grooveplayer.presentation.common.rememberPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.library.ui.AlbumItemComponent
import com.aethelsoft.grooveplayer.presentation.library.ui.SongItemComponent
import com.aethelsoft.grooveplayer.utils.S_PADDING
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {

    val playerViewModel = rememberPlayerViewModel()
    val songs by viewModel.songs.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(query) {
        viewModel.search(query)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(XBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                indicator = { tabPositions ->
                    TabRowDefaults.PrimaryIndicator()
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Songs") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Albums") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Artists") }
                )
            }
            
            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                selectedTab == 0 -> {
                    // All results
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (songs.isNotEmpty()) {
                            item {
                                Text(
                                    "Songs",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(songs) { song ->
                                SongItemComponent(
                                    song = song,
                                    onClick = {
                                        scope.launch {
                                            viewModel.searchRepository.saveSongClick(
                                                song.id,
                                                song.title,
                                                song.artist,
                                                song.artworkUrl
                                            )
                                            // Play the song
                                            val allSongs = viewModel.getAllSongs()
                                            val songIndex = allSongs.indexOfFirst { it.id == song.id }
                                            if (songIndex >= 0) {
                                                playerViewModel.setQueue(allSongs, songIndex)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        if (albums.isNotEmpty()) {
                            item {
                                Text(
                                    "Albums",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(albums) { albumName ->
                                SearchAlbumItem(
                                    albumName = albumName,
                                    viewModel = viewModel,
                                    onClick = {
                                        scope.launch {
                                            // Find artist for this album
                                            val allSongs = viewModel.getAllSongs()
                                            val albumSong = allSongs.find { it.album?.name == albumName }
                                            val artistName = albumSong?.artist ?: ""
                                            val artworkUrl = viewModel.getAlbumArtwork(albumName, artistName)
                                            viewModel.searchRepository.saveAlbumClick(albumName, artistName, artworkUrl)
                                        }
                                        // TODO: Navigate to album
                                    }
                                )
                            }
                        }
                        
                        if (artists.isNotEmpty()) {
                            item {
                                Text(
                                    "Artists",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(artists) { artistName ->
                                SearchArtistItem(
                                    artistName = artistName,
                                    viewModel = viewModel,
                                    onClick = {
                                        scope.launch {
                                            val artworkUrl = viewModel.getArtistArtwork(artistName)
                                            viewModel.searchRepository.saveArtistClick(artistName, artworkUrl)
                                        }
                                        // TODO: Navigate to artist
                                    }
                                )
                            }
                        }
                        
                        if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No results found")
                                }
                            }
                        }
                    }
                }
                selectedTab == 1 -> {
                    // Songs only
                    if (songs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No songs found")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(songs) { song ->
                                SongItemComponent(
                                    song = song,
                                    onClick = {
                                        scope.launch {
                                            viewModel.searchRepository.saveSongClick(
                                                song.id,
                                                song.title,
                                                song.artist,
                                                song.artworkUrl
                                            )
                                            // Play the song
                                            val allSongs = viewModel.getAllSongs()
                                            val songIndex = allSongs.indexOfFirst { it.id == song.id }
                                            if (songIndex >= 0) {
                                                playerViewModel.setQueue(allSongs, songIndex)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                selectedTab == 2 -> {
                    // Albums only
                    if (albums.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No albums found")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(albums) { albumName ->
                                SearchAlbumItem(
                                    albumName = albumName,
                                    viewModel = viewModel,
                                    onClick = {
                                        scope.launch {
                                            val allSongs = viewModel.getAllSongs()
                                            val albumSong = allSongs.find { it.album?.name == albumName }
                                            val artistName = albumSong?.artist ?: ""
                                            val artworkUrl = viewModel.getAlbumArtwork(albumName, artistName)
                                            viewModel.searchRepository.saveAlbumClick(albumName, artistName, artworkUrl)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                selectedTab == 3 -> {
                    // Artists only
                    if (artists.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No artists found")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(artists) { artistName ->
                                SearchArtistItem(
                                    artistName = artistName,
                                    viewModel = viewModel,
                                    onClick = {
                                        scope.launch {
                                            val artworkUrl = viewModel.getArtistArtwork(artistName)
                                            viewModel.searchRepository.saveArtistClick(artistName, artworkUrl)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAlbumItem(
    albumName: String,
    viewModel: SearchViewModel,
    onClick: () -> Unit
) {
    var artworkUrl by remember { mutableStateOf<String?>(null) }
    var artistName by remember { mutableStateOf<String>("") }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(albumName) {
        // Find artist for this album
                                            val allSongs = viewModel.getAllSongs()
                                            val albumSong = allSongs.find { it.album?.name == albumName }
        artistName = albumSong?.artist ?: ""
        artworkUrl = viewModel.getAlbumArtwork(albumName, artistName)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(S_PADDING))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = albumName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (artistName.isNotEmpty()) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchArtistItem(
    artistName: String,
    viewModel: SearchViewModel,
    onClick: () -> Unit
) {
    var artworkUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(artistName) {
        artworkUrl = viewModel.getArtistArtwork(artistName)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(S_PADDING))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = artistName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}
