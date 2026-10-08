package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.settings.R

@Suppress("TooManyFunctions")
internal class FetchingMailSettingsOptionsMapper(
    private val resources: StringsResourceManager,
) {
    private val localFolderSizeOptions = persistentListOf(
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
        SelectOption(0.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_all)
        },
    )

    private val syncMessageFromOptions = persistentListOf(
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

    private val fetchMessageUpToOptions = persistentListOf(
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
    private val folderPollFrequencyOptions = persistentListOf(
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
    private val eraseDeletedMessageOnServerOptions = persistentListOf(
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

    private val whenIDeleteAMessageOptions = persistentListOf(
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

    private val maxFolderToCheckWithPushOptions = persistentListOf(
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

    private val localFolderSizeOptionsById = localFolderSizeOptions.associateBy { it.id }
    private val syncMessageFromOptionsById = syncMessageFromOptions.associateBy { it.id }
    private val fetchMessageUpToOptionsById = fetchMessageUpToOptions.associateBy { it.id }
    private val folderPollFrequencyOptionsById = folderPollFrequencyOptions.associateBy { it.id }
    private val eraseDeletedMessageOnServerOptionsById = eraseDeletedMessageOnServerOptions.associateBy { it.id }
    private val whenIDeleteAMessageOptionsById = whenIDeleteAMessageOptions.associateBy { it.id }
    private val maxFolderToCheckWithPushOptionsById = maxFolderToCheckWithPushOptions().associateBy { it.id }
    private val refreshIdleConnectionOptionsById = refreshIdleConnectionOptions().associateBy { it.id }

    private val defaultLocalFolderSizeOption = localFolderSizeOptionsById.getValue(DEFAULT_LOCAL_FOLDER_SIZE_OPTION_ID)

    private val defaultSyncMessageFromOption = syncMessageFromOptionsById.getValue(DEFAULT_SYNC_MESSAGE_FROM_OPTION_ID)

    private val defaultFetchMessageUpToOption =
        fetchMessageUpToOptionsById.getValue(DEFAULT_FETCH_MESSAGE_UP_TO_OPTION_ID)

    private val defaultFolderPollFrequencyOption =
        folderPollFrequencyOptionsById.getValue(DEFAULT_FOLDER_POLL_FREQUENCY_OPTION_ID)
    private val defaultEraseDeletedMessageOnServerOption =
        eraseDeletedMessageOnServerOptionsById.getValue(Expunge.EXPUNGE_IMMEDIATELY.name)

    private val defaultWhenIDeleteAMessageOption = whenIDeleteAMessageOptionsById.getValue(DeletePolicy.ON_DELETE.name)
    private val defaultMaxFolderToCheckWithPushOption =
        maxFolderToCheckWithPushOptionsById.getValue(DEFAULT_MAX_FOLDER_CHECK_WITH_OPTION_ID)

    private val defaultRefreshIdleConnectionOption =
        refreshIdleConnectionOptionsById.getValue(DEFAULT_REFRESH_IDLE_CONNECTION_OPTION_ID)

    fun localFolderSizeOptions() = localFolderSizeOptions
    fun syncMessageFromOption() = syncMessageFromOptions
    fun fetchMessageUpToOptions() = fetchMessageUpToOptions

    fun folderPollFrequencyOptions() = folderPollFrequencyOptions
    fun eraseDeletedMessageOnServerOptions() = eraseDeletedMessageOnServerOptions
    fun whenIDeleteAMessageOptions() = whenIDeleteAMessageOptions
    fun maxFolderToCheckWithPushOptions() = maxFolderToCheckWithPushOptions
    fun refreshIdleConnectionOptions() = refreshIdleConnectionOptions

    fun defaultLocalFolderSizeOption(): SelectOption = defaultLocalFolderSizeOption

    fun defaultSyncMessageFromOption(): SelectOption = defaultSyncMessageFromOption

    fun defaultFetchMessageUpToOption(): SelectOption = defaultFetchMessageUpToOption

    fun defaultFolderPollFrequencyOption(): SelectOption = defaultFolderPollFrequencyOption

    fun defaultEraseDeletedMessageOnServerOption(): SelectOption = defaultEraseDeletedMessageOnServerOption

    fun whenIDeleteAMessageOption(): SelectOption = defaultWhenIDeleteAMessageOption

    fun defaultMaxFolderToCheckWithPushOption(): SelectOption = defaultMaxFolderToCheckWithPushOption
    fun defaultRefreshIdleConnectionOption(): SelectOption = defaultRefreshIdleConnectionOption

    fun localFolderSizeOption(value: Int): SelectOption =
        localFolderSizeOptionsById[value.toString()] ?: defaultLocalFolderSizeOption

    fun syncMessageFromOption(value: Int): SelectOption =
        syncMessageFromOptionsById[value.toString()] ?: defaultSyncMessageFromOption

    fun fetchMessageUpToOption(value: Int): SelectOption =
        fetchMessageUpToOptionsById[value.toString()] ?: defaultFetchMessageUpToOption

    fun folderPollFrequencyOption(value: Int): SelectOption =
        folderPollFrequencyOptionsById[value.toString()] ?: defaultFolderPollFrequencyOption

    fun eraseDeletedMessageOnServerOption(value: String): SelectOption =
        eraseDeletedMessageOnServerOptionsById[value] ?: defaultEraseDeletedMessageOnServerOption

    fun whenIDeleteAMessageOption(value: String): SelectOption =
        whenIDeleteAMessageOptionsById[value] ?: defaultWhenIDeleteAMessageOption

    fun maxFolderToCheckWithPushOption(value: String): SelectOption =
        maxFolderToCheckWithPushOptionsById[value] ?: defaultMaxFolderToCheckWithPushOption

    fun refreshIdleConnectionOption(value: String): SelectOption =
        refreshIdleConnectionOptionsById[value] ?: defaultRefreshIdleConnectionOption

    private companion object {
        const val DEFAULT_LOCAL_FOLDER_SIZE_OPTION_ID = "10"
        const val DEFAULT_SYNC_MESSAGE_FROM_OPTION_ID = "-1"
        const val DEFAULT_FETCH_MESSAGE_UP_TO_OPTION_ID = "0"
        const val DEFAULT_FOLDER_POLL_FREQUENCY_OPTION_ID = "-1"
        const val DEFAULT_MAX_FOLDER_CHECK_WITH_OPTION_ID = "5"
        const val DEFAULT_REFRESH_IDLE_CONNECTION_OPTION_ID = "60"
    }
}
