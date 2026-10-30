package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.SettingsBuilder
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.State

@Composable
internal fun SyncAndNotificationSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    provider: SettingViewProvider,
    builder: SettingsBuilder,
    modifier: Modifier = Modifier,
) {
    val settings = remember(state, builder, onEvent) {
        builder.build(state = state, onEvent = onEvent)
    }

    provider.SettingView(
        title = stringResource(R.string.account_settings_notificaction_and_sync),
        subtitle = null,
        settings = settings,
        onSettingValueChange = { setting -> handleSettingChange(setting, onEvent) },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
        actions = {
            // TODO 11682
        },
    )
}

private fun handleSettingChange(
    setting: Setting,
    onEvent: (Event) -> Unit,
) {
    when (setting) {
        is SettingValue.Select -> onEvent(Event.OnEnablePushNotificationChange(setting.value))
        else -> Unit
    }
}
