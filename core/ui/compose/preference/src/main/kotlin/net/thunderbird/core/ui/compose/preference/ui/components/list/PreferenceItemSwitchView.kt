package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Switch
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceItemSwitchView(
    preference: PreferenceSetting.Switch,
    modifier: Modifier = Modifier,
    onPreferenceChange: (PreferenceSetting<*>) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(MainTheme.spacings.double),
    ) {
        Column(
            Modifier.weight(1f),
        ) {
            TextTitleMedium(text = preference.title())
            preference.description()?.let {
                TextBodyMedium(text = it)
            }
        }
        Switch(
            checked = preference.value,
            onCheckedChange = {
                onPreferenceChange(preference.copy(value = it))
            },
            enabled = preference.enabled,
        )
    }
}
