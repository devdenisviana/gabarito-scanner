package com.devdenisviana.gabaritoscanner.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** A row of tabs with a content area that cross-fades when the tab changes. */
@Composable
fun TabbedContent(
    titles: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (tab: Int) -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selected, containerColor = Color.Transparent) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selected == index,
                    onClick = { selected = index },
                    text = { Text(title) },
                )
            }
        }
        Crossfade(
            targetState = selected,
            animationSpec = tween(250),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "tabContent",
        ) { tab ->
            content(tab)
        }
    }
}