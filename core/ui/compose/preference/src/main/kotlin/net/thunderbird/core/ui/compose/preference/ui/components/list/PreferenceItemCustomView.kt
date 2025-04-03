package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.compose.preference.api.PreferenceDisplay

@Composable
internal fun PreferenceItemCustomView(
    preference: PreferenceDisplay.Custom,
    modifier: Modifier = Modifier,
) {
    preference.customUi(modifier)
}
