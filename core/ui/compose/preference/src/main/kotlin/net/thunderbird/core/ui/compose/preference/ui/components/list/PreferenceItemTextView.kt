package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

@Composable
internal fun PreferenceItemTextView(
    preference: PreferenceSetting.Text,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceItemLayout(
        onClick = onClick,
        icon = preference.icon,
        modifier = modifier,
    ) {
        TextTitleMedium(text = preference.title)
        TextBodyMedium(text = preference.value)
    }
}
