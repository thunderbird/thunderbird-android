package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

@Composable
internal fun GeneralSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    provider: SettingViewProvider,
    modifier: Modifier = Modifier,
) {
    provider.SettingView(
        title = stringResource(R.string.account_settings_general_title),
        subtitle = state.subtitle,
        settings = state.settings,
        onSettingValueChange = { setting ->
            onEvent(Event.OnSettingValueChange(setting))
        },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
    )
}
