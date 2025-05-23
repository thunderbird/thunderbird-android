package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.RadioGroup
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceItemRadioGroupSingleChoiceView(
    preference: PreferenceSetting.SingleChoice,
    onPreferenceChange: (PreferenceSetting<*>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MainTheme.spacings.double),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        TextTitleMedium(text = preference.title())

        RadioGroup(
            onClick = {
                onPreferenceChange(preference.copy(value = it))
            },
            options = preference.options,
            optionTitle = { it.title() },
            selectedOption = preference.value,
        )

        Row {
            Spacer(modifier = Modifier.weight(1f))
            ButtonText(
                text = preference.cancelButtonTitle(),
                onClick = onCancel,
            )
        }
    }
}
