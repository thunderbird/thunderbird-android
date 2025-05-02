package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceDisplay
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceItem(
    preference: Preference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (preference) {
        // PreferenceSetting
        is PreferenceSetting.Text -> {
            PreferenceItemTextView(
                preference = preference,
                onClick = onClick,
                modifier = modifier,
            )
        }

        is PreferenceSetting.Color -> {
            PreferenceItemColorView(
                preference = preference,
                onClick = onClick,
                modifier = modifier,
            )
        }

        // PreferenceDisplay
        is PreferenceDisplay.Custom -> {
            PreferenceItemCustomView(
                preference = preference,
                modifier = modifier,
            )
        }
    }
}
