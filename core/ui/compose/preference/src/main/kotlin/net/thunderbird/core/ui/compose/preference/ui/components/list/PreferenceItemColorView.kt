package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.core.ui.compose.preference.ui.components.common.ColorView

@Composable
internal fun PreferenceItemColorView(
    preference: PreferenceSetting.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceItemLayout(
        onClick = onClick,
        icon = preference.icon(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
            ) {
                TextTitleMedium(text = preference.title())
                preference.description()?.let {
                    TextBodyMedium(text = it)
                }
            }
            ColorView(
                color = preference.value,
                onClick = null,
                modifier = Modifier.padding(start = MainTheme.spacings.default),
            )
        }
    }
}
