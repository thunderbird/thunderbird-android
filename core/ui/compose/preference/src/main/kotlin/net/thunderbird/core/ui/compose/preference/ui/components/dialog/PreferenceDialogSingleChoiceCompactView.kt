package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.RadioGroup
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceDialogSingleChoiceCompactView(
    preference: PreferenceSetting.SingleChoiceCompact,
    onConfirmClick: (PreferenceSetting<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by remember { mutableStateOf(preference.options) }
    var selectedOption by remember { mutableStateOf(preference.value) }

    PreferenceDialogLayout(
        title = preference.title(),
        icon = preference.icon(),
        onConfirmClick = {
            onConfirmClick(preference.copy(value = selectedOption))
        },
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        preference.description()?.let {
            TextBodyMedium(text = it)

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        }

        RadioGroup(
            onClick = { selectedOption = it },
            options = options,
            optionTitle = { it.title() },
            selectedOption = selectedOption,
        )
    }
}
