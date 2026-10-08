package net.thunderbird.feature.account.settings.impl.ui.fetchingMail.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.SettingsBuilder
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.State
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsId

@Suppress("LongMethod")
@Composable
internal fun AdvancedFetchingMailSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    provider: SettingViewProvider,
    builder: SettingsBuilder,
    modifier: Modifier = Modifier,
) {
    val settings = remember(state, builder, onEvent) {
        builder.buildAdvancedFetchingMailSettings(state = state, onEvent = onEvent)
    }

    provider.SettingView(
        title = stringResource(R.string.account_settings_push_advanced_title),
        subtitle = state.subtitle,
        settings = settings,
        onSettingValueChange = { setting ->
            handleSettingChange(setting, onEvent)
        },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
    )
}

private fun handleSettingChange(
    setting: Setting,
    onEvent: (Event) -> Unit,
) {
    when (setting) {
        is SettingValue.Switch -> {
            when (setting.id) {
                FetchingMailSettingsId.SYNC_SERVER_DELETIONS -> onEvent(
                    Event.OnSyncServerDeletionsToggle(setting.value),
                )

                FetchingMailSettingsId.MARK_AS_READ_WHEN_DELETED -> onEvent(
                    Event.OnMarkAsReadWhenDeletedToggle(setting.value),
                )

                else -> Unit
            }
        }

        is SettingValue.Select -> {
            when (setting.id) {
                FetchingMailSettingsId.MAX_FOLDER_TO_CHECK_WITH_PUSH -> {
                    onEvent(Event.OnMaxFolderToCheckWithPushChange(setting.value))
                }

                FetchingMailSettingsId.REFRESH_IDLE_CONNECTION -> {
                    onEvent(Event.OnRefreshIdleConnectionFrequencyChange(setting.value))
                }

                else -> Unit
            }
        }

        else -> Unit
    }
}
