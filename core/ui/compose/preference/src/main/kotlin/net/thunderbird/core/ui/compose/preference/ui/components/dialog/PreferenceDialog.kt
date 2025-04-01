package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceDialog(
    preference: Preference,
    onConfirmClick: (PreferenceSetting<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(preference is PreferenceSetting<*>) {
        "Unsupported preference type: ${preference::class.java.simpleName}"
    }

    when (preference) {
        is PreferenceSetting.Text -> {
            PreferenceDialogTextView(
                preference = preference,
                onConfirmClick = onConfirmClick,
                onDismissClick = onDismissClick,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            )
        }

        is PreferenceSetting.Color -> TODO()
    }
}
