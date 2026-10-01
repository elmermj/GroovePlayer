package com.aethelworks.grooveplayer.presentation.profile.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aethelworks.grooveplayer.domain.model.RecentUpdates
import com.aethelworks.grooveplayer.presentation.common.grooveBottomContentInset
import com.aethelworks.grooveplayer.utils.S_PADDING
import com.aethelworks.grooveplayer.utils.theme.ui.GrooveTheme
import com.aethelworks.grooveplayer.utils.theme.ui.SoftWhite

@Composable
fun RecentUpdatesBody(
    updates: RecentUpdates,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            Text(
                text = updates.intro,
                style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                color = SoftWhite,
            )
        }
        updates.sections.forEach { section ->
            item {
                ProfileSectionComponent(sectionTitle = section.title) {
                    section.items.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.height(S_PADDING))
                        Text(
                            text = item.title,
                            style = GrooveTheme.typography.sectionItemTitle.toTextStyle(),
                            color = GrooveTheme.colors.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.detail,
                            style = GrooveTheme.typography.sectionItemSubtitle.toTextStyle(),
                            color = SoftWhite,
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(grooveBottomContentInset(includeMiniPlayer = true)))
        }
    }
}
