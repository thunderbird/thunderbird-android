package net.thunderbird.core.ui.setting

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.setting.SettingValue.CompactSelectSingleOption.CompactOption
import net.thunderbird.core.ui.setting.SettingValue.SelectSingleOption.Option

/**
 * A setting that holds a value of type [T].
 */
sealed interface SettingValue<T> : Setting {
    val value: T
    val requiresEditView: Boolean

    /**
     * A setting that holds a string value.
     *
     * This requires an edit view to modify the value.
     *
     * @param id The unique identifier for the setting.
     * @param title A lambda that returns the title of the setting.
     * @param description A lambda that returns the description of the setting. Default is null.
     * @param icon A lambda that returns the icon of the setting as an [ImageVector]. Default is null.
     * @param value The current value of the setting.
     * @param transform A function that transforms the entered string value. Default is the identity function.
     */
    data class Text(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: String,
        val transform: (String) -> String = { it },
    ) : SettingValue<String> {
        override val requiresEditView: Boolean = true
    }

    /**
     * A setting that holds a color value.
     *
     * This requires an edit view to select a color from the provided list of colors.
     *
     * @param id The unique identifier for the setting.
     * @param title A lambda that returns the title of the setting.
     * @param description A lambda that returns the description of the setting. Default is null.
     * @param icon A lambda that returns the icon of the setting as an [ImageVector]. Default is null.
     * @param value The current color value of the setting, represented as an integer.
     */
    data class Color(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: Int,
        val colors: ImmutableList<Int>,
    ) : SettingValue<Int> {
        override val requiresEditView: Boolean = true
    }

    /**
     * A setting that allows the user to select a single option from a list of options.
     *
     * The options are displayed in a compact manner, suitable for scenarios where space is limited.
     * The number of options must be between 2 and 4.
     *
     * This requires no edit view to modify the value. The selection can be made directly from the setting item.
     *
     * @param id The unique identifier for the setting.
     * @param title A lambda that returns the title of the setting.
     * @param description A lambda that returns the description of the setting. Default is null.
     * @param value The currently selected option.
     * @param options The list of available options to choose from.
     */
    data class CompactSelectSingleOption<T>(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        override val value: CompactOption<T>,
        val options: ImmutableList<CompactOption<T>>,
    ) : SettingValue<CompactOption<T>> {
        override val requiresEditView: Boolean = false

        init {
            require(options.size >= 2) { "There must be at least two options." }
            require(options.size <= 4) { "There can be at most four options." }
        }

        data class CompactOption<T>(
            val id: String,
            val title: () -> String,
            val value: T,
        )
    }

    /**
     * A setting that allows the user to select a single option from a list of options.
     *
     * Requires an edit view to select the option from the provided list of options.
     *
     * @param id The unique identifier for the setting.
     * @param title A lambda that returns the title of the setting.
     * @param description A lambda that returns the description of the setting. Default is null.
     * @param icon A lambda that returns the icon of the setting as an [ImageVector]. Default is null.
     * @param value The currently selected option.
     * @param options The list of available options to choose from.
     */
    data class SelectSingleOption(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: Option,
        val options: ImmutableList<Option>,
    ) : SettingValue<Option> {
        override val requiresEditView: Boolean = true

        data class Option(
            val id: String,
            val title: () -> String,
        )
    }

    /**
     * A setting that holds a boolean value.
     *
     * This does not require an edit view to modify the value. The value can be toggled directly from the setting..
     *
     * @param id The unique identifier for the setting.
     * @param title A lambda that returns the title of the setting.
     * @param description A lambda that returns the description of the setting. Default is null.
     * @param value The current boolean value of the setting.
     */
    data class Switch(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        override val value: Boolean,
    ) : SettingValue<Boolean> {
        override val requiresEditView: Boolean = false
    }
}
