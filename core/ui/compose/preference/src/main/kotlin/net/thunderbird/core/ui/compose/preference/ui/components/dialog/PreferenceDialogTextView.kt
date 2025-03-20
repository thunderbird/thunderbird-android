package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceDialogTextView(
    preference: PreferenceSetting.Text,
    onConfirmClick: (PreferenceSetting<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPreference = remember { mutableStateOf(preference) }

    PreferenceDialogLayout(
        title = preference.title,
        icon = preference.icon,
        onConfirmClick = {
            onConfirmClick(currentPreference.value)
        },
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        preference.description?.let {
            TextBodyMedium(text = it)

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        }

        TextInput(
            text = currentPreference.value.value,
            contentPadding = PaddingValues(),
            onTextChange = { currentPreference.value = currentPreference.value.copy(value = it) },
        )
    }
}
