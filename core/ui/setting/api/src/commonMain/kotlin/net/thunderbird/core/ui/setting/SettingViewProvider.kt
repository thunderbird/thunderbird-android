package net.thunderbird.core.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A provider of a settings view.
 */
interface SettingViewProvider {

    /**
     * A view that displays a list of settings.
     *
     * @param title The title of the view.
     * @param subtitle The subtitle of the view (optional).
     * @param settings The list of settings to display.
     * @param onSettingValueChange The callback to be invoked when a setting value is changed.
     * @param onBack The callback to be invoked when the back button is clicked.
     * @param modifier The modifier to be applied to the view.
     */
    @Composable
    fun SettingView(
        title: String,
        settings: Settings,
        onSettingValueChange: (SettingValue<*>) -> Unit,
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
    )
}
