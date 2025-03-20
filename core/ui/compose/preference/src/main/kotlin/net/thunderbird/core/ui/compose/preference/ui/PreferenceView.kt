package net.thunderbird.core.ui.compose.preference.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

/**
 * A view that displays a list of preferences.
 *
 * @param title The title of the view.
 * @param subtitle The subtitle of the view (optional).
 * @param preferences The list of preferences to display.
 * @param onPreferenceChange The callback to be invoked when a preference is changed.
 * @param onBack The callback to be invoked when the back button is clicked.
 * @param modifier The modifier to be applied to the view.
 */
@Composable
fun PreferenceView(
    title: String,
    preferences: ImmutableList<Preference>,
    onPreferenceChange: (PreferenceSetting<*>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    PreferenceViewWithDialog(
        title = title,
        subtitle = subtitle,
        preferences = preferences,
        onPreferenceChange = onPreferenceChange,
        onBack = onBack,
        modifier = modifier,
    )
