package net.thunderbird.components.ui.bolt.molecule.tab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.PreviewWithThemeLightDark
import net.thunderbird.components.ui.bolt.atom.tab.TabPrimary
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * A molecule component that displays a scrollable row of primary tabs.
 *
 * This is a wrapper around Material 3's [PrimaryScrollableTabRow] that is pre-configured with the
 * design system's styling. It is designed to be used with [TabPrimary] composables.
 *
 * It is used to display a scrollable horizontal row of tabs, where each tab corresponds to a
 * different page or view.
 *
 * @param selectedTabIndex The index of the currently selected tab.
 * @param modifier The modifier to be applied to the tab row.
 * @param edgePadding The padding between the start and end of the tab row and the tabs.
 * @param indicatorColor The color of the indicator that highlights the selected tab.
 * @param indicatorColor The color of the indicator.
 * @param tabs The composable content of the tabs. This should be a list of [TabPrimary] composables.
 *
 * @see PrimaryScrollableTabRow
 * @see TabPrimary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabRowPrimary(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = TabRowPrimaryDefaults.EdgePadding,
    indicatorWidth: Dp = TabRowPrimaryDefaults.IndicatorWidth,
    indicatorColor: Color = TabRowPrimaryDefaults.IndicatorColor,
    tabs: @Composable () -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = edgePadding,
        contentColor = BoltTheme.colors.onSurfaceVariant,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false),
                width = indicatorWidth,
                color = indicatorColor,
            )
        },
        modifier = modifier.fillMaxWidth(),
        tabs = tabs,
    )
}

object TabRowPrimaryDefaults {
    val EdgePadding = TabRowDefaults.ScrollableTabRowEdgeStartPadding
    val IndicatorWidth = Dp.Unspecified
    val IndicatorColor
        @Composable
        get() = BoltTheme.colors.outline
}

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
            indicatorColor = BoltTheme.colors.tertiary,
            indicatorWidth = BoltTheme.sizes.medium,
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
            edgePadding = BoltTheme.spacings.zero,
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
