package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.settings.R

@Suppress("TooManyFunctions")
class FetchingMailSettingsBuilder(
    private val resources: StringsResourceManager,
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

    val localFolderSizeOptions = persistentListOf(
        SelectOption(10.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_10)
        },
        SelectOption(25.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_25)
        },
        SelectOption(50.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_50)
        },
        SelectOption(100.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_100)
        },
        SelectOption(250.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_250)
        },
        SelectOption(500.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_500)
        },
        SelectOption(1000.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_1000)
        },
        SelectOption(2500.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_2500)
        },
        SelectOption(5000.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_5000)
        },
        SelectOption(10000.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_10000)
        },
        SelectOption("all") {
            resources.stringResource(R.string.account_settings_options_mail_display_count_all)
        },
    )

    val syncMessageFromOptions = persistentListOf(
        SelectOption("-1") {
            resources.stringResource(R.string.account_settings_message_age_any)
        },
        SelectOption("0") {
            resources.stringResource(R.string.account_settings_message_age_0)
        },
        SelectOption("1") {
            resources.stringResource(R.string.account_settings_message_age_1)
        },
        SelectOption("2") {
            resources.stringResource(R.string.account_settings_message_age_2)
        },
        SelectOption("7") {
            resources.stringResource(R.string.account_settings_message_age_7)
        },
        SelectOption("14") {
            resources.stringResource(R.string.account_settings_message_age_14)
        },
        SelectOption("21") {
            resources.stringResource(R.string.account_settings_message_age_21)
        },
        SelectOption("28") {
            resources.stringResource(R.string.account_settings_message_age_1_month)
        },
        SelectOption("56") {
            resources.stringResource(R.string.account_settings_message_age_2_months)
        },
        SelectOption("84") {
            resources.stringResource(R.string.account_settings_message_age_3_months)
        },
        SelectOption("168") {
            resources.stringResource(R.string.account_settings_message_age_6_months)
        },
        SelectOption("365") {
            resources.stringResource(R.string.account_settings_message_age_1_year)
        },
    )

    val fetchMessageUpToOptions = persistentListOf(
        SelectOption("1024") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_1)
        },
        SelectOption("2048") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_2)
        },
        SelectOption("4096") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_4)
        },
        SelectOption("8192") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_8)
        },
        SelectOption("16384") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_16)
        },
        SelectOption("32768") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_32)
        },
        SelectOption("65536") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_64)
        },
        SelectOption("131072") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_128)
        },
        SelectOption("262144") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_256)
        },
        SelectOption("524288") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_512)
        },
        SelectOption("1048576") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_1024)
        },
        SelectOption("2097152") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_2048)
        },
        SelectOption("5242880") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_5120)
        },
        SelectOption("10485760") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_10240)
        },
        SelectOption("0") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_any)
        },
    )

    val folderPollFrequencyOptions = persistentListOf(
        SelectOption("-1") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_never)
        },
        SelectOption("15") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_15min)
        },
        SelectOption("30") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_30min)
        },
        SelectOption("60") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_1hour)
        },
        SelectOption("120") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_2hour)
        },
        SelectOption("180") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_3hour)
        },
        SelectOption("360") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_6hour)
        },
        SelectOption("720") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_12hour)
        },
        SelectOption("1440") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_24hour)
        },
    )

    val eraseDeletedMessageOnServerOptions = persistentListOf(
        SelectOption(Expunge.EXPUNGE_IMMEDIATELY.name) {
            resources.stringResource(R.string.account_settings_expunge_policy_immediately)
        },
        SelectOption(Expunge.EXPUNGE_ON_POLL.name) {
            resources.stringResource(R.string.account_settings_expunge_policy_on_poll)
        },
        SelectOption(Expunge.EXPUNGE_MANUALLY.name) {
            resources.stringResource(R.string.account_settings_expunge_policy_manual)
        },
    )

    val whenIDeleteAMessageOptions = persistentListOf(
        SelectOption(DeletePolicy.NEVER.name) {
            resources.stringResource(R.string.account_settings_incoming_delete_policy_never_label)
        },
        SelectOption(DeletePolicy.ON_DELETE.name) {
            resources.stringResource(R.string.account_settings_incoming_delete_policy_delete_label)
        },
        SelectOption(DeletePolicy.MARK_AS_READ.name) {
            resources.stringResource(R.string.account_settings_incoming_delete_policy_markread_label)
        },
    )

    val maxFolderToCheckWithPushOptions = persistentListOf(
        SelectOption("5") {
            resources.stringResource(R.string.account_settings_push_limit_5)
        },
        SelectOption("10") {
            resources.stringResource(R.string.account_settings_push_limit_10)
        },
        SelectOption("25") {
            resources.stringResource(R.string.account_settings_push_limit_25)
        },
        SelectOption("50") {
            resources.stringResource(R.string.account_settings_push_limit_50)
        },
        SelectOption("100") {
            resources.stringResource(R.string.account_settings_push_limit_100)
        },
        SelectOption("250") {
            resources.stringResource(R.string.account_settings_push_limit_250)
        },
        SelectOption("500") {
            resources.stringResource(R.string.account_settings_push_limit_500)
        },
        SelectOption("1000") {
            resources.stringResource(R.string.account_settings_push_limit_1000)
        },
    )

    val refreshIdleConnectionOptions = persistentListOf(
        SelectOption("2") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_2min)
        },
        SelectOption("3") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_3min)
        },
        SelectOption("6") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_6min)
        },
        SelectOption("12") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_12min)
        },
        SelectOption("24") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_24min)
        },
        SelectOption("36") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_36min)
        },
        SelectOption("48") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_48min)
        },
        SelectOption("60") {
            resources.stringResource(R.string.account_settings_idle_refresh_period_60min)
        },

    )

    private fun localFolderSize(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.LOCAL_FOLDER_SIZE,
        title = { resources.stringResource(R.string.account_settings_mail_display_count_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = localFolderSizeOptions,
    )

    private fun syncMessageFrom(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.SYNC_MESSAGE_FROM,
        title = { resources.stringResource(R.string.account_settings_message_age_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = syncMessageFromOptions,
    )

    private fun fetchMessageUpTo(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.FETCH_MESSAGE_UP_TO,
        title = { resources.stringResource(R.string.account_settings_autodownload_message_size_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = fetchMessageUpToOptions,
    )

    private fun folderPollFrequency(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.FOLDER_POLL_FREQUENCY,
        title = { resources.stringResource(R.string.account_settings_mail_check_frequency_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = folderPollFrequencyOptions,
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
        options = whenIDeleteAMessageOptions,
    )

    private fun eraseDeletedMessageOnServer(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.ERASE_DELETED_MESSAGE_ON_SERVER,
        title = { resources.stringResource(R.string.account_settings_expunge_policy_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = eraseDeletedMessageOnServerOptions,
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
        options = maxFolderToCheckWithPushOptions,
    )

    private fun refreshIdleConnection(value: SelectOption): Setting = SettingValue.Select(
        id = FetchingMailSettingsId.REFRESH_IDLE_CONNECTION,
        title = { resources.stringResource(R.string.account_settings_idle_refresh_period_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = refreshIdleConnectionOptions,
    )
}
