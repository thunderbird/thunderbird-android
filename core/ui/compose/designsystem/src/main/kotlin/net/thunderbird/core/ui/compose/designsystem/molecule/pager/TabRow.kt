package net.thunderbird.core.ui.compose.designsystem.molecule.pager

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.pager.Tab

/**
 * A molecule component that displays a row of tabs.
 *
 * This is a wrapper around Material 3's [PrimaryScrollableTabRow] that uses the design system's [Tab] component.
 * It is used to display a scrollable horizontal row of tabs, where each tab corresponds to a different page or
 * view.
 *
 * @param selectedTabIndex The index of the currently selected tab. This should be the current page
 *  index from a `PagerState`.
 * @param modifier The modifier to be applied to the tab row.
 * @param tabs The composable content of the tabs. This should be a list of `Tab` composables.
 *
 * @see androidx.compose.material3.PrimaryScrollableTabRow
 * @see net.thunderbird.core.ui.compose.designsystem.atom.pager.Tab
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = MainTheme.spacings.zero,
        contentColor = MainTheme.colors.onSurfaceVariant,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false),
                width = Dp.Unspecified,
                color = MainTheme.colors.outline,
            )
        },
        modifier = modifier.fillMaxWidth(),
        tabs = tabs,
    )
}
