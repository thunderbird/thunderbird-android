package net.thunderbird.core.ui.setting

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.setting.SettingValue.SegmentedButton.SegmentedButtonOption
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption

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
     * @param validate A function that validates the entered string value and returns an error message if invalid. Default is always valid.
     */
    data class Text(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: String,
        val transform: (String) -> String = { it },
        val validate: (String) -> String? = { null },
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
     * A setting that allows the user to select a single option from a segmented button.
     *
     * The number of options must be between 2 and 4.
     * This does not require an edit view to modify the value, as the selection is made directly.
     *
     * @param id The unique identifier for the setting.
     * @param title A lambda that returns the title of the setting.
     * @param description A lambda that returns the description of the setting. Default is null.
     * @param value The currently selected option.
     * @param options The list of available options to choose from.
     */
    data class SegmentedButton<T>(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        override val value: SegmentedButtonOption<T>,
        val options: ImmutableList<SegmentedButtonOption<T>>,
    ) : SettingValue<SegmentedButtonOption<T>> {
        override val requiresEditView: Boolean = false

        init {
            require(options.size >= 2) { "There must be at least two options." }
            require(options.size <= 4) { "There can be at most four options." }
        }

        data class SegmentedButtonOption<T>(
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
    data class Select(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: SelectOption,
        val options: ImmutableList<SelectOption>,
    ) : SettingValue<SelectOption> {
        override val requiresEditView: Boolean = true

        data class SelectOption(
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
