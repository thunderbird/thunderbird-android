package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonSegmentedSingleChoice
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceItemSingleChoiceView(
    preference: PreferenceSetting.SingleChoice,
    onPreferenceChange: (PreferenceSetting<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MainTheme.spacings.double),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        TextTitleMedium(text = preference.title())

        ButtonSegmentedSingleChoice(
            onClick = {
                onPreferenceChange(preference.copy(value = it))
            },
            options = preference.options,
            optionTitle = { it.title() },
            selectedOption = preference.value,
        )

        preference.description()?.let {
            TextBodyMedium(
                modifier = Modifier.padding(start = MainTheme.spacings.oneHalf),
                color = MainTheme.colors.onSurfaceVariant,
                text = it,
            )
        }
    }
}
