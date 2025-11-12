package net.thunderbird.core.ui.compose.designsystem.template.pager

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the configuration for a single tab within a tab-based layout, like a horizontal pager.
 *
 * @property title The text to be displayed on the tab.
 */
sealed interface TabConfig {
    val title: String
}

/**
 * Configuration for a primary-styled tab within a horizontal pager.
 * This tab style can include an icon, a content description for accessibility, and a badge count.
 *
 * @param title The text to be displayed on the tab.
 * @param icon The optional icon to be displayed on the tab, usually placed before the title.
 * @param contentDescription Optional content description for the tab's icon, used for accessibility.
 *  If null, the title will be used.
 * @param badgeCount An optional integer to display in a badge on the tab, e.g., for notifications.
 *  A null or zero value will hide the badge.
 */
data class TabPrimaryConfig(
    override val title: String,
    val icon: ImageVector? = null,
    val contentDescription: String? = null,
    val badgeCount: Int? = null,
) : TabConfig

/**
 * Configuration for a single secondary tab within a horizontal pager.
 *
 * Secondary tabs are a simpler variant, displaying only a title.
 *
 * @param title The text to be displayed on the tab.
 */
data class TabSecondaryConfig(override val title: String) : TabConfig
