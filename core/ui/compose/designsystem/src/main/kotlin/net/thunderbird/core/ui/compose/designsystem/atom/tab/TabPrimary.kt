package net.thunderbird.core.ui.compose.designsystem.atom.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.theme2.LocalContentColor
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.Tab as Material3Tab

/**
 * UI atom for a single tab within a tab. This is a wrapper around the Material 3 [androidx.compose.material3.Tab]
 * composable, providing a more specific API for our design system.
 *
 * @param selected Whether this tab is currently selected.
 * @param title The composable lambda for the title of the tab.
 * @param onClick The callback to be invoked when this tab is clicked.
 * @param modifier The [Modifier] to be applied to this tab.
 * @param icon The optional composable lambda for the icon of the tab.
 * @param badge The optional composable lambda for the badge of the tab, displayed on top of the icon.
 * @param badgeColor The background color for the badge.
 * @param enabled Controls the enabled state of this tab. When `false`, this tab will not be
 * clickable and will appear disabled.
 */
@Composable
fun TabPrimary(
    selected: Boolean,
    title: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    badgeColor: Color = MainTheme.colors.primaryContainer,
    enabled: Boolean = true,
) {
    Material3Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = title,
        icon = icon?.let { icon ->
            { TabIcon(icon, badge = badge, badgeColor = badgeColor) }
        },
    )
}

@Composable
private fun TabIcon(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (BoxScope.() -> Unit)? = null,
    badgeColor: Color = Color.Unspecified,
) {
    Box(modifier = modifier) {
        icon()
        badge?.let { badge ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = MainTheme.spacings.default, y = -MainTheme.spacings.half)
                    .clip(CircleShape)
                    .background(color = badgeColor, shape = CircleShape)
                    .padding(MainTheme.spacings.quarter)
                    .widthIn(min = MainTheme.sizes.badge),
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColorFor(badgeColor)) {
                    badge()
                }
            }
        }
    }
}
