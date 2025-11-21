package net.thunderbird.core.ui.compose.designsystem.molecule.tab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.tab.TabPrimary

@PreviewLightDark
@Composable
private fun TabRowPrimaryPreview() {
    PreviewWithThemeLightDark {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3") }
        TabRowPrimary(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                TabPrimary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TabRowPrimaryIndicatorChangesPreview() {
    PreviewWithThemeLightDark {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3") }
        TabRowPrimary(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            indicatorColor = MainTheme.colors.tertiary,
            indicatorWidth = MainTheme.sizes.medium,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabPrimary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TabRowPrimaryEdgePaddingPreview() {
    PreviewWithThemeLightDark {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5") }
        TabRowPrimary(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = MainTheme.spacings.zero,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabPrimary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
}
