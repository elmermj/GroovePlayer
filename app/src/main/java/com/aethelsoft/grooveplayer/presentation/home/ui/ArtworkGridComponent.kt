package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun ArtworkGrid(
    artworks: List<String>
) {
    val count = artworks.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        // 2x1 grid (2 items)
        if (count == 2) {
            items(
                count = 2,
                key = { index -> "${index}_${artworks[index]}" }
            ) { index ->
                ArtworkImage(
                    url = artworks[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f)
                )
            }
            return@LazyVerticalGrid
        }

        // 3 items in 2x2 grid (1 empty cell)
        // More than 3 items → take 4 (full 2x2)
        items(
            count = 4,
            key = { index -> 
                if (index < artworks.size) "${index}_${artworks[index]}" 
                else "empty_$index"
            }
        ) { index ->
            if (index < artworks.size) {
                ArtworkImage(
                    url = artworks[index],
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                )
            } else {
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ArtworkImage(
    url: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}