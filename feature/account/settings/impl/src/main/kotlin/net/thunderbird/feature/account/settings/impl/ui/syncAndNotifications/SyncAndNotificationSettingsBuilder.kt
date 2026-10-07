package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationsSettingsId.NOTIFICATION_STYLE
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationsSettingsId.FOLDER_SYNC_AND_PUSH
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationsSettingsId.FETCHING_AND_DATA_LIMITS

class SyncAndNotificationSettingsBuilder(
    private val resources: StringsResourceManager,
) : SyncAndNotificationSettingsContract.SettingsBuilder {
    override fun build(
        state: SyncAndNotificationSettingsContract.State,
        onEvent: (SyncAndNotificationSettingsContract.Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()
        settings += notificationStyle(onEvent)
        settings += folderSyncAndPush(onEvent)
        settings += fetchingAndDataLimits(onEvent)
        return settings.toImmutableList()
    }

    // TODO #11683: add enable account push SettingValue.Select for push for inbox

    private fun notificationStyle(onEvent: (SyncAndNotificationSettingsContract.Event) -> Unit): Setting =
        SettingValue.ActionText(
            id = NOTIFICATION_STYLE,
            title = { resources.stringResource(R.string.account_settings_notification_style) },
            description = { null },
            icon = { null },
            value = resources.stringResource(R.string.account_settings_notification_style_description),
            onClick = {
                onEvent(SyncAndNotificationSettingsContract.Event.OnSelectNotificationStyle)
            },
        )
    private fun folderSyncAndPush(onEvent: (SyncAndNotificationSettingsContract.Event) -> Unit): Setting =
        SettingValue.ActionText(
            id = FOLDER_SYNC_AND_PUSH,
            title = { resources.stringResource(R.string.account_settings_folder_sync_and_push) },
            description = { null },
            icon = { null },
            value = resources.stringResource(R.string.account_settings_folder_sync_and_push_description),
            onClick = {
                onEvent(SyncAndNotificationSettingsContract.Event.OnSelectFolderSyncAndPush)
            },
        )
    private fun fetchingAndDataLimits(onEvent: (SyncAndNotificationSettingsContract.Event) -> Unit): Setting =
        SettingValue.ActionText(
            id = FETCHING_AND_DATA_LIMITS,
            title = { resources.stringResource(R.string.account_settings_fetching_and_data_limits) },
            description = { null },
            icon = { null },
            value = resources.stringResource(R.string.account_settings_fetching_and_data_limits_description),
            onClick = {
                onEvent(SyncAndNotificationSettingsContract.Event.OnSelectFetchingAndDataLimits)
            },
        )
}
