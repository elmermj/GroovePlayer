package com.aethelsoft.grooveplayer.presentation.library.songs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.presentation.library.songs.metadata.EditSongMetadataViewModel
import com.aethelsoft.grooveplayer.utils.theme.icons.XBack
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun EditSongMetadataDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (Song) -> Unit,
    viewModel: EditSongMetadataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(song.id) {
        viewModel.loadMetadata(song)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Blur background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() }
            )
            
            // Dialog content
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Song Metadata",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(XBack, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Content
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            EditTitleField(
                                title = uiState.title,
                                onTitleChange = { viewModel.updateTitle(it) }
                            )
                        }
                        
                        item {
                            EditGenresField(
                                genres = uiState.genres,
                                suggestions = uiState.genreSuggestions,
                                onGenresChange = { viewModel.updateGenres(it) },
                                onSearchGenres = { viewModel.searchGenres(it) }
                            )
                        }
                        
                        item {
                            EditArtistsField(
                                artists = uiState.artists,
                                suggestions = uiState.artistSuggestions,
                                onArtistsChange = { viewModel.updateArtists(it) },
                                onSearchArtists = { viewModel.searchArtists(it) }
                            )
                        }
                        
                        item {
                            EditAlbumField(
                                album = uiState.album,
                                suggestions = uiState.albumSuggestions,
                                onAlbumChange = { viewModel.updateAlbum(it) },
                                onSearchAlbums = { viewModel.searchAlbums(it) }
                            )
                        }
                        
                        item {
                            EditYearField(
                                year = uiState.year,
                                useAlbumYear = uiState.useAlbumYear,
                                onYearChange = { viewModel.updateYear(it) },
                                onUseAlbumYearChange = { viewModel.updateUseAlbumYear(it) }
                            )
                        }
                        
                        item {
                            EditTrackNumberField(
                                trackNumber = uiState.trackNumber,
                                onTrackNumberChange = { viewModel.updateTrackNumber(it) }
                            )
                        }
                        
                        item {
                            EditArtworkField(
                                artworkBytes = uiState.artworkBytes,
                                onArtworkChange = { bytes, mimeType -> viewModel.updateArtwork(bytes, mimeType) }
                            )
                        }
                    }
                    
                    if (uiState.saveError != null) {
                        Text(
                            text = uiState.saveError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Footer buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (uiState.isSaving) return@Button
                                coroutineScope.launch {
                                    val ok = viewModel.saveMetadata()
                                    if (!ok) return@launch
                                    val newAlbum = uiState.album?.let { newName ->
                                        val existing = song.album
                                        if (existing != null) {
                                            existing.copy(id = com.aethelsoft.grooveplayer.domain.model.makeAlbumId(existing.artist, newName), name = newName)
                                        } else {
                                            com.aethelsoft.grooveplayer.domain.model.Album(
                                                id = com.aethelsoft.grooveplayer.domain.model.makeAlbumId(
                                                    uiState.artists.firstOrNull() ?: song.artist,
                                                    newName
                                                ),
                                                name = newName,
                                                artist = uiState.artists.firstOrNull() ?: song.artist,
                                                artworkUrl = song.artworkUrl,
                                                songs = emptyList(),
                                                year = uiState.year ?: song.year
                                            )
                                        }
                                    }
                                    onSave(
                                        song.copy(
                                            title = uiState.title,
                                            artist = uiState.artists.firstOrNull() ?: song.artist,
                                            genre = uiState.genres.firstOrNull() ?: song.genre,
                                            album = newAlbum
                                        )
                                    )
                                    onDismiss()
                                }
                            }
                        ) {
                            Text(if (uiState.isSaving) "Saving…" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTitleField(
    title: String,
    onTitleChange: (String) -> Unit
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun EditGenresField(
    genres: List<String>,
    suggestions: List<String>,
    onGenresChange: (List<String>) -> Unit,
    onSearchGenres: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        Text(
            text = "Genres",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Selected genres as chips
        if (genres.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = genres.size,
                    key = { index -> genres[index] }
                ) { index ->
                    val genre = genres[index]
                    InputChip(
                        selected = true,
                        onClick = {
                            onGenresChange(genres - genre)
                        },
                        label = { Text(genre) },
                        trailingIcon = {
                            Icon(XBack, contentDescription = "Remove")
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Search field with dropdown
        Box {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchGenres(it)
                    expanded = it.isNotBlank() && suggestions.isNotEmpty()
                },
                label = { Text("Add genre") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                trailingIcon = {
                    if (expanded) {
                        IconButton(onClick = { expanded = false }) {
                            Icon(XBack, contentDescription = "Close")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (searchQuery.isNotBlank() && !genres.contains(searchQuery)) {
                            onGenresChange(genres + searchQuery)
                            searchQuery = ""
                            expanded = false
                            keyboardController?.hide()
                        }
                    }
                )
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                suggestions.forEach { suggestion ->
                    if (!genres.contains(suggestion)) {
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                onGenresChange(genres + suggestion)
                                searchQuery = ""
                                expanded = false
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditArtistsField(
    artists: List<String>,
    suggestions: List<String>,
    onArtistsChange: (List<String>) -> Unit,
    onSearchArtists: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        Text(
            text = "Artists",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Selected artists as chips
        if (artists.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = artists.size,
                    key = { index -> artists[index] }
                ) { index ->
                    val artist = artists[index]
                    InputChip(
                        selected = true,
                        onClick = {
                            onArtistsChange(artists - artist)
                        },
                        label = { Text(artist) },
                        trailingIcon = {
                            Icon(XBack, contentDescription = "Remove")
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Search field with dropdown
        Box {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchArtists(it)
                    expanded = it.isNotBlank() && suggestions.isNotEmpty()
                },
                label = { Text("Add artist") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (expanded) {
                        IconButton(onClick = { expanded = false }) {
                            Icon(XBack, contentDescription = "Close")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (searchQuery.isNotBlank() && !artists.contains(searchQuery)) {
                            onArtistsChange(artists + searchQuery)
                            searchQuery = ""
                            expanded = false
                            keyboardController?.hide()
                        }
                    }
                )
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                suggestions.forEach { suggestion ->
                    if (!artists.contains(suggestion)) {
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                onArtistsChange(artists + suggestion)
                                searchQuery = ""
                                expanded = false
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAlbumField(
    album: String?,
    suggestions: List<String>,
    onAlbumChange: (String?) -> Unit,
    onSearchAlbums: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(album ?: "") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        Text(
            text = "Album",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onAlbumChange(it.ifBlank { null })
                    onSearchAlbums(it)
                    expanded = it.isNotBlank() && suggestions.isNotEmpty()
                },
                label = { Text("Album name") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onAlbumChange(null)
                        }) {
                            Icon(XBack, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        expanded = false
                        keyboardController?.hide()
                    }
                )
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            searchQuery = suggestion
                            onAlbumChange(suggestion)
                            expanded = false
                            keyboardController?.hide()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditTrackNumberField(
    trackNumber: Int?,
    onTrackNumberChange: (Int?) -> Unit
) {
    var trackText by remember { mutableStateOf(trackNumber?.toString() ?: "") }
    LaunchedEffect(trackNumber) {
        trackText = trackNumber?.toString() ?: ""
    }
    OutlinedTextField(
        value = trackText,
        onValueChange = {
            trackText = it
            onTrackNumberChange(it.toIntOrNull())
        },
        label = { Text("Track number") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun EditArtworkField(
    artworkBytes: ByteArray?,
    onArtworkChange: (ByteArray?, String?) -> Unit
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                onArtworkChange(bytes, mimeType)
            }
        }
    }
    
    Column {
        Text(
            text = "Artwork",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context).data(artworkBytes).build()
                        ),
                        contentDescription = "Album artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "No artwork",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { imagePicker.launch("image/*") }) {
                    Text("Change artwork")
                }
                if (artworkBytes != null) {
                    TextButton(onClick = { onArtworkChange(null, null) }) {
                        Text("Remove artwork")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditYearField(
    year: Int?,
    useAlbumYear: Boolean,
    onYearChange: (Int?) -> Unit,
    onUseAlbumYearChange: (Boolean) -> Unit
) {
    var yearText by remember { mutableStateOf(year?.toString() ?: "") }
    
    Column {
        Text(
            text = "Year",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = useAlbumYear,
                onCheckedChange = onUseAlbumYearChange
            )
            Text(
                text = "Use album year",
                modifier = Modifier.weight(1f)
            )
        }
        
        if (!useAlbumYear) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = yearText,
                onValueChange = {
                    yearText = it
                    onYearChange(it.toIntOrNull())
                },
                label = { Text("Year") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}

