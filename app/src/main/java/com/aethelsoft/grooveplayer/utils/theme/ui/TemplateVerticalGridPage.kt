package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aethelsoft.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelsoft.grooveplayer.presentation.common.rememberClearMiniPlayer
import com.aethelsoft.grooveplayer.presentation.common.topBarContentInset
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING

@Composable
fun TemplateVeritcalGridPage(
    columns: Int = 8,
    contents: LazyGridScope.() -> Unit
){
    val gridState = rememberLazyGridState()
    val initialSpacerHeight = topBarContentInset()
    val bottomSpacerHeight = grooveBottomContentInset(includeMiniPlayer = rememberClearMiniPlayer())

    Column {
        LazyVerticalGrid(
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(M_PADDING),
            verticalArrangement = Arrangement.spacedBy(M_PADDING),
            columns = GridCells.Fixed(columns),
            modifier = Modifier.padding(
                start = M_PADDING,
                end = M_PADDING
            )
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .padding(top = initialSpacerHeight, bottom = S_PADDING)
                ) { }
            }
            contents()
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(bottomSpacerHeight))
            }
        }
    }
}
