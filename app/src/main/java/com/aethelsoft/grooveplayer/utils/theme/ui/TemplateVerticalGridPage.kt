package com.aethelsoft.grooveplayer.utils.theme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.common.LocalPlayerViewModel
import com.aethelsoft.grooveplayer.presentation.home.ui.LastPlayedSectionComponent
import com.aethelsoft.grooveplayer.presentation.home.ui.LibraryCardComponent
import com.aethelsoft.grooveplayer.utils.APP_BAR_HEIGHT
import com.aethelsoft.grooveplayer.utils.M_PADDING
import com.aethelsoft.grooveplayer.utils.S_PADDING
import kotlin.collections.ifEmpty

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateVeritcalGridPage(
    contents: LazyGridScope.() -> Unit
){
    val gridState = rememberLazyGridState()
    val xContentWindowInsets = contentWindowInsets
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(xContentWindowInsets) }

    /* ---------- INITIAL SPACER HEIGHT ---------- */
    val initialSpacerHeight =
        APP_BAR_HEIGHT +
                safeInsets.insets.asPaddingValues().calculateTopPadding() +
                12.dp
    val bottomSpacerHeight = safeInsets.insets.asPaddingValues().calculateBottomPadding()

    Column{
        LazyVerticalGrid(
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(M_PADDING),
            verticalArrangement = Arrangement.spacedBy(M_PADDING),
            columns = GridCells.Fixed(8),
            modifier = Modifier.padding(
                top = 20.dp,
                start = M_PADDING,
                end = M_PADDING
            )
        ) {
            item(span = { GridItemSpan(maxLineSpan) }){
                Box(
                    modifier = Modifier
                        .padding(top = initialSpacerHeight, bottom = S_PADDING)
                ) { }
            }
            contents()
            item(span = { GridItemSpan(maxLineSpan) }){
                Spacer(modifier = Modifier.height(bottomSpacerHeight + 106.dp))
            }
        }
    }
    
}