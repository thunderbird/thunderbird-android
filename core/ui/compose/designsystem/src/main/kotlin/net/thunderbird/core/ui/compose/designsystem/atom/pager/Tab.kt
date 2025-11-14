package net.thunderbird.core.ui.compose.designsystem.atom.pager

import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * UI atom for a single tab within a tab. This is a wrapper around the Material 3 [Tab]
 * composable, providing a more specific API for our design system.
 *
 * @param selected Whether this tab is currently selected.
 * @param title The composable lambda for the title of the tab.
 * @param onClick The callback to be invoked when this tab is clicked.
 * @param modifier The [Modifier] to be applied to this tab.
 * @param icon The optional composable lambda for the icon of the tab.
 * @param enabled Controls the enabled state of this tab. When `false`, this tab will not be
 * clickable and will appear disabled.
 */
@Composable
fun Tab(
    selected: Boolean,
    title: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = title,
        icon = icon,
    )
}
