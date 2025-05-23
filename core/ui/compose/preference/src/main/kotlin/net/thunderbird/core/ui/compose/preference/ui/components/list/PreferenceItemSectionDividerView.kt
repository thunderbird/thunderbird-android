package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import net.thunderbird.core.ui.compose.preference.api.PreferenceDisplay

@Composable
internal fun PreferenceItemSectionDividerView(
    preference: PreferenceDisplay.SectionDivider,
    modifier: Modifier = Modifier,
) {
    DividerHorizontal(
        modifier = modifier,
        color = preference.color(),
        thickness = preference.thickness(),
    )
}
