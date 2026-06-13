package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.settings.R

@Suppress("TooManyFunctions")
internal class FetchingMailSettingsBuilder(
    private val resources: StringsResourceManager,
    private val fetchingMailSettingsOptionsMapper: FetchingMailSettingsOptionsMapper,
) : FetchingMailSettingsContract.SettingsBuilder {

    override fun buildCoreFetchingMailSettings(
        state: FetchingMailSettingsContract.State,
        onEvent: (FetchingMailSettingsContract.Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()
        settings += localFolderSize(value = state.localFolderSize)
        settings += syncMessageFrom(value = state.syncMessageFrom)
        settings += fetchMessageUpTo(value = state.fetchMessageUpTo)
        settings += folderPollFrequency(value = state.folderPollFrequency)
        settings += syncServerDeletions(value = state.syncServerDeletions)
        settings += markAsReadWhenDeleted(value = state.markAsReadWhenDeleted)
        settings += whenIDeleteAMessage(value = state.whenIDeleteAMessage)
        settings += eraseDeletedMessageOnServer(
            value = state.eraseDeletedMessageOnServer,
        )
        settings += incomingServer(onEvent = onEvent)
        settings += advanced(onEvent = onEvent)

        return settings.toImmutableList()
    }

    override fun buildAdvancedFetchingMailSettings(
        state: FetchingMailSettingsContract.State,
        onEvent: (FetchingMailSettingsContract.Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()
        settings += maxFolderToCheckWithPush(value = state.maxFolderToCheckWithPush)
        settings += refreshIdleConnection(value = state.refreshIdleConnection)
        return settings.toImmutableList()
    }

    private fun localFolderSize(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.LOCAL_FOLDER_SIZE,
        title = { resources.stringResource(R.string.account_settings_mail_display_count_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.localFolderSizeOptions(),
    )

    private fun syncMessageFrom(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.SYNC_MESSAGE_FROM,
        title = { resources.stringResource(R.string.account_settings_message_age_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.syncMessageFromOption(),
    )

    private fun fetchMessageUpTo(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.FETCH_MESSAGE_UP_TO,
        title = { resources.stringResource(R.string.account_settings_autodownload_message_size_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.fetchMessageUpToOptions(),
    )

    private fun folderPollFrequency(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.FOLDER_POLL_FREQUENCY,
        title = { resources.stringResource(R.string.account_settings_mail_check_frequency_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.folderPollFrequencyOptions(),
    )

    private fun syncServerDeletions(value: Boolean): Setting = SettingValue.Switch(
        id = FetchingMailSettingsId.SYNC_SERVER_DELETIONS,
        title = { resources.stringResource(R.string.account_settings_sync_remote_deletetions_label) },
        description = { resources.stringResource(R.string.account_settings_sync_remote_deletetions_summary) },
        value = value,
    )

    private fun markAsReadWhenDeleted(value: Boolean): Setting = SettingValue.Switch(
        id = FetchingMailSettingsId.MARK_AS_READ_WHEN_DELETED,
        title = { resources.stringResource(R.string.account_settings_mark_message_as_read_on_delete_label) },
        description = { resources.stringResource(R.string.account_settings_mark_message_as_read_on_delete_summary) },
        value = value,
    )

    private fun whenIDeleteAMessage(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.WHEN_I_DELETE_A_MESSAGE,
        title = { resources.stringResource(R.string.account_settings_incoming_delete_policy_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.whenIDeleteAMessageOptions(),
    )

    private fun eraseDeletedMessageOnServer(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.ERASE_DELETED_MESSAGE_ON_SERVER,
        title = { resources.stringResource(R.string.account_settings_expunge_policy_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.eraseDeletedMessageOnServerOptions(),
    )

    private fun incomingServer(onEvent: (FetchingMailSettingsContract.Event) -> Unit): Setting =
        SettingValue.ActionText(
            id = FetchingMailSettingsId.IN_COMING_SERVER,
            title = { resources.stringResource(R.string.account_settings_incoming_label) },
            description = { null },
            icon = { null },
            value = resources.stringResource(R.string.account_settings_incoming_summary),
            onClick = { onEvent(FetchingMailSettingsContract.Event.OnInComingServerClick) },
        )

    private fun advanced(onEvent: (FetchingMailSettingsContract.Event) -> Unit): Setting = SettingValue.ActionText(
        id = FetchingMailSettingsId.ADVANCE,
        title = { resources.stringResource(R.string.account_settings_push_advanced_title) },
        description = { null },
        icon = { null },
        value = "",
        onClick = { onEvent(FetchingMailSettingsContract.Event.OnAdvanceClick) },
    )

    private fun maxFolderToCheckWithPush(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.MAX_FOLDER_TO_CHECK_WITH_PUSH,
        title = { resources.stringResource(R.string.account_settings_push_limit_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.maxFolderToCheckWithPushOptions(),
    )

    private fun refreshIdleConnection(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.REFRESH_IDLE_CONNECTION,
        title = { resources.stringResource(R.string.account_settings_idle_refresh_period_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchingMailSettingsOptionsMapper.refreshIdleConnectionOptions(),
    )
}
