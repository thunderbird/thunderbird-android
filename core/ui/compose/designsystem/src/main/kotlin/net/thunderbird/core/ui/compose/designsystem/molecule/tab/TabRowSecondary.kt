package net.thunderbird.core.ui.compose.designsystem.molecule.tab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.tab.TabSecondary

/**
 * A molecule component that displays a scrollable row of secondary tabs.
 *
 * This is a wrapper around Material 3's [SecondaryScrollableTabRow] that is pre-configured with the
 * design system's styling. It is designed to be used with [TabSecondary] composables.
 *
 * It is used to display a scrollable horizontal row of tabs, where each tab corresponds to a
 * different page or view.
 *
 * @param selectedTabIndex The index of the currently selected tab.
 * @param modifier The modifier to be applied to the tab row.
 * @param edgePadding The padding between the start and end of the tab row and the tabs.
 * @param indicatorColor The color of the indicator that highlights the selected tab.
 * @param tabs The composable content of the tabs. This should be a list of [TabSecondary] composables.
 *
 * @see SecondaryScrollableTabRow
 * @see TabSecondary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabRowSecondary(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = TabRowSecondaryDefaults.EdgePadding,
    indicatorColor: Color = TabRowSecondaryDefaults.IndicatorColor,
    tabs: @Composable () -> Unit,
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = edgePadding,
        contentColor = MainTheme.colors.onSurfaceVariant,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false),
                color = indicatorColor,
            )
        },
        modifier = modifier.fillMaxWidth(),
        tabs = tabs,
    )
}

object TabRowSecondaryDefaults {
    val EdgePadding = TabRowDefaults.ScrollableTabRowEdgeStartPadding
    val IndicatorColor @Composable get() = MainTheme.colors.outline
}
