package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.core.ui.compose.preference.ui.PreferenceView
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

@Composable
internal fun GeneralSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceView(
        title = stringResource(R.string.account_settings_general_title),
        subtitle = state.subtitle,
        preferences = state.preferences,
        onPreferenceChange = { preference ->
            onEvent(Event.OnPreferenceSettingChange(preference))
        },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
    )
}
