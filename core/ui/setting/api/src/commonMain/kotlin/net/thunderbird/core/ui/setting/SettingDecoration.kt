package net.thunderbird.core.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A setting decoration could be used for enhancing the UI of a settings screen. It does not hold any value.
 *
 * Examples include section headers, dividers, or custom UI components.
 */
sealed interface SettingDecoration : Setting {

    /**
     * A setting that displays custom UI.
     */
    data class Custom(
        override val id: String,
        val customUi: @Composable (Modifier) -> Unit,
    ) : SettingDecoration

    /**
     * A setting that displays a section header.
     */
    data class SectionHeader(
        override val id: String,
        val title: () -> String,
        val color: () -> Color = { Color.Unspecified },
    ) : SettingDecoration

    /**
     * A setting that displays a divider.
     */
    data class SectionDivider(
        override val id: String,
    ) : SettingDecoration
}
