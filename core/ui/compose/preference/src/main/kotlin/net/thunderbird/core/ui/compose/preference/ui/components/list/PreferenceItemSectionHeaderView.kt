package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceDisplay

@Composable
internal fun PreferenceItemSectionHeaderView(
    preference: PreferenceDisplay.SectionHeader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(MainTheme.spacings.double)) {
        TextTitleMedium(
            text = preference.title(),
            color = preference.color(),
        )
    }
}
