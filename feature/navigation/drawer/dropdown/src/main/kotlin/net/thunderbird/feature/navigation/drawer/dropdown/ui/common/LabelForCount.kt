package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import android.content.res.Resources
import net.thunderbird.feature.navigation.drawer.dropdown.R

@Suppress("MagicNumber")
internal fun labelForCount(
    count: Int,
    resources: Resources,
) = when {
    count in 1..99 -> "$count"

    count in 100..1000 -> resources.getString(
        R.string.navigation_drawer_dropdown_folder_item_badge_count_greater_than_99,
    )

    count > 1000 -> resources.getString(
        R.string.navigation_drawer_dropdown_folder_item_badge_count_greater_than_1_000,
    )

    else -> ""
}
