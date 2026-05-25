package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.State

private const val TAG = "FetchingMailSettingsViewModel"

@Suppress("LargeClass")
internal class FetchingMailSettingsViewModel(
    private val accountId: AccountId,
    private val logger: Logger,
    private val resources: StringsResourceManager,
    private val getAccountName: UseCase.GetAccountName,
    private val getLegacyAccount: UseCase.GetLegacyAccount,
    private val updateFetchingMailSettings: UseCase.UpdateFetchingMailSettings,
    initialState: State = State(
        localFolderSize = SelectOption(10.toString()) {
            resources.stringResource(R.string.account_settings_options_mail_display_count_10)
        },
        syncMessageFrom = SelectOption("-1") {
            resources.stringResource(R.string.account_settings_message_age_any)
        },
        fetchMessageUpTo = SelectOption("1024") {
            resources.stringResource(R.string.account_settings_autodownload_message_size_1)
        },
        folderPollFrequency = SelectOption("-1") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_never)
        },
        syncServerDeletions = false,
        markAsReadWhenDeleted = false,
        whenIDeleteAMessage = SelectOption("-1") {
            resources.stringResource(R.string.account_settings_incoming_delete_policy_never_label)
        },
        eraseDeletedMessageOnServer = SelectOption("0") {
            resources.stringResource(R.string.account_settings_expunge_policy_immediately)
        },
        maxFolderToCheckWithPush = SelectOption("5") {
            resources.stringResource(R.string.account_settings_push_limit_5)
        },
        refreshIdleConnection = SelectOption("15") {
            resources.stringResource(R.string.account_settings_options_mail_check_frequency_15min)
        },

    ),
) : BaseViewModel<State, Event, Effect>(initialState = initialState), FetchingMailSettingsContract.ViewModel {

    init {
        observeFetchingMailSettings()
        observeAccountName()
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun event(event: Event) {
        when (event) {
            is Event.OnBackPressed -> {
                emitEffect(Effect.NavigateBack)
            }

            is Event.OnLocalFolderSizeChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateLocalFolderSize(
                                event.localFolderSize.id.toInt(),
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(localFolderSize = event.localFolderSize) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnSyncMessageFromChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateSyncMessageFrom(
                                event.syncMessageFrom.id.toInt(),
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(syncMessageFrom = event.syncMessageFrom) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnFetchMessageUpToChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateFetchMessageUpTo(
                                event.fetchMessageUpTo.id.toInt(),
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(fetchMessageUpTo = event.fetchMessageUpTo) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnFolderPollFrequencyChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateFolderPollFrequency(
                                event.folderPollFrequency.id.toInt(),
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(folderPollFrequency = event.folderPollFrequency) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnSyncServerDeletionsToggle -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateSyncServerDeletions(
                                event.syncServerDeletions,
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(syncServerDeletions = event.syncServerDeletions) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnMarkAsReadWhenDeletedToggle -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateMarkAsReadWhenDeleted(
                                event.markAsReadWhenDeleted,
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(markAsReadWhenDeleted = event.markAsReadWhenDeleted) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnWhenIDeleteAMessageChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateWhenIDeleteAMessage(
                                event.whenIDeleteAMessage.id,
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(whenIDeleteAMessage = event.whenIDeleteAMessage) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnEraseDeletedMessageOnServerChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateEraseDeletedMessageOnServer(
                                event.eraseDeletedMessageOnServer.id,
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state ->
                                state.copy(eraseDeletedMessageOnServer = event.eraseDeletedMessageOnServer)
                            }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnInComingServerClick -> {
                emitEffect(Effect.NavigateToIncomingServerSettings)
            }

            is Event.OnAdvanceClick -> {
                emitEffect(Effect.NavigateToAdvancedFetchingMailSettings)
            }

            is Event.OnMaxFolderToCheckWithPushChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateOnMaxFolderToCheckWithPushChange(
                                event.maxFolderToCheckWithPushChanges.id.toInt(),
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state ->
                                state.copy(maxFolderToCheckWithPush = event.maxFolderToCheckWithPushChanges)
                            }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }

            is Event.OnRefreshIdleConnectionFrequencyChange -> {
                viewModelScope.launch {
                    updateFetchingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateFetchingMailSettingsCommand.UpdateRefreshIdleConnectionFrequencyChange(
                                event.refreshIdleConnectionFrequency.id.toInt(),
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state ->
                                state.copy(refreshIdleConnection = event.refreshIdleConnectionFrequency)
                            }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun observeFetchingMailSettings() {
        viewModelScope.launch {
            getLegacyAccount(accountId).handle(
                onSuccess = {
                    val localFolderSize = when (it.displayCount) {
                        10 -> {
                            SelectOption(10.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_10)
                            }
                        }

                        25 -> {
                            SelectOption(25.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_25)
                            }
                        }

                        50 -> {
                            SelectOption(50.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_50)
                            }
                        }

                        100 -> {
                            SelectOption(100.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_100)
                            }
                        }

                        250 -> {
                            SelectOption(250.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_250)
                            }
                        }

                        500 -> {
                            SelectOption(500.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_500)
                            }
                        }

                        1000 -> {
                            SelectOption(1000.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_1000)
                            }
                        }

                        2500 -> {
                            SelectOption(2500.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_2500)
                            }
                        }

                        5000 -> {
                            SelectOption(5000.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_5000)
                            }
                        }

                        10000 -> {
                            SelectOption(10000.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_10000)
                            }
                        }

                        else -> {
                            SelectOption("all") {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_all)
                            }
                        }
                    }
                    val fetchMessageUpTo = when (it.maximumAutoDownloadMessageSize) {
                        1024 -> {
                            SelectOption(1024.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_1)
                            }
                        }

                        2048 -> {
                            SelectOption(2048.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_2)
                            }
                        }

                        4096 -> {
                            SelectOption(4096.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_4)
                            }
                        }

                        8192 -> {
                            SelectOption(8192.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_8)
                            }
                        }

                        16384 -> {
                            SelectOption(16384.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_16)
                            }
                        }

                        32768 -> {
                            SelectOption(32768.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_32)
                            }
                        }

                        65536 -> {
                            SelectOption(65536.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_64)
                            }
                        }

                        131072 -> {
                            SelectOption(131072.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_128)
                            }
                        }

                        262144 -> {
                            SelectOption(262144.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_256)
                            }
                        }

                        524288 -> {
                            SelectOption(524288.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_512)
                            }
                        }

                        1048576 -> {
                            SelectOption(1048576.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_1024)
                            }
                        }

                        2097152 -> {
                            SelectOption(2097152.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_2048)
                            }
                        }

                        5242880 -> {
                            SelectOption(5242880.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_5120)
                            }
                        }

                        10485760 -> {
                            SelectOption(10485760.toString()) {
                                resources.stringResource(R.string.account_settings_autodownload_message_size_10240)
                            }
                        }

                        else -> {
                            SelectOption(0.toString()) {
                                resources.stringResource(R.string.account_settings_options_mail_display_count_all)
                            }
                        }
                    }
                    val syncMessageFrom = when (it.maximumPolledMessageAge) {
                        0 -> {
                            SelectOption("0") {
                                resources.stringResource(R.string.account_settings_message_age_0)
                            }
                        }

                        1 -> {
                            SelectOption("1") {
                                resources.stringResource(R.string.account_settings_message_age_1)
                            }
                        }

                        2 -> {
                            SelectOption("2") {
                                resources.stringResource(R.string.account_settings_message_age_2)
                            }
                        }

                        7 -> {
                            SelectOption("7") {
                                resources.stringResource(R.string.account_settings_message_age_7)
                            }
                        }

                        14 -> {
                            SelectOption("14") {
                                resources.stringResource(R.string.account_settings_message_age_14)
                            }
                        }

                        21 -> {
                            SelectOption("21") {
                                resources.stringResource(R.string.account_settings_message_age_21)
                            }
                        }

                        28 -> {
                            SelectOption("28") {
                                resources.stringResource(R.string.account_settings_message_age_1_month)
                            }
                        }

                        56 -> {
                            SelectOption("56") {
                                resources.stringResource(R.string.account_settings_message_age_2_months)
                            }
                        }

                        84 -> {
                            SelectOption("84") {
                                resources.stringResource(R.string.account_settings_message_age_3_months)
                            }
                        }

                        168 -> {
                            SelectOption("168") {
                                resources.stringResource(R.string.account_settings_message_age_6_months)
                            }
                        }

                        365 -> {
                            SelectOption("365") {
                                resources.stringResource(R.string.account_settings_message_age_1_year)
                            }
                        }

                        else -> {
                            SelectOption("-1") {
                                resources.stringResource(R.string.account_settings_message_age_any)
                            }
                        }
                    }
                    val folderPollFrequency = when (it.automaticCheckIntervalMinutes) {
                        15 -> {
                            SelectOption("15") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_15min)
                            }
                        }

                        30 -> {
                            SelectOption("30") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_30min)
                            }
                        }

                        60 -> {
                            SelectOption("60") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_1hour)
                            }
                        }

                        120 -> {
                            SelectOption("120") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_2hour)
                            }
                        }

                        180 -> {
                            SelectOption("180") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_3hour)
                            }
                        }

                        360 -> {
                            SelectOption("360") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_6hour)
                            }
                        }

                        720 -> {
                            SelectOption("720") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_12hour)
                            }
                        }

                        1440 -> {
                            SelectOption("1440") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_24hour)
                            }
                        }

                        else -> {
                            SelectOption("-1") {
                                resources.stringResource(R.string.account_settings_options_mail_check_frequency_never)
                            }
                        }
                    }
                    val syncServerDeletions = it.isSyncRemoteDeletions
                    val markReadWhenDeleted = it.isMarkMessageAsReadOnDelete
                    val whenIDeleteAMessage = when (it.deletePolicy.name) {
                        "NEVER" -> {
                            SelectOption(DeletePolicy.NEVER.name) {
                                resources.stringResource(R.string.account_settings_incoming_delete_policy_never_label)
                            }
                        }

                        "ON_DELETE" -> {
                            SelectOption(DeletePolicy.ON_DELETE.name) {
                                resources.stringResource(R.string.account_settings_incoming_delete_policy_delete_label)
                            }
                        }

                        "MARK_AS_READ" -> {
                            SelectOption(DeletePolicy.MARK_AS_READ.name) {
                                resources.stringResource(
                                    R.string.account_settings_incoming_delete_policy_markread_label,
                                )
                            }
                        }

                        else -> {
                            error("Invalid message delete policy")
                        }
                    }
                    val eraseDeletedMessageOnServer = when (it.expungePolicy.name) {
                        "EXPUNGE_IMMEDIATELY" -> {
                            SelectOption(Expunge.EXPUNGE_IMMEDIATELY.name) {
                                resources.stringResource(R.string.account_settings_expunge_policy_immediately)
                            }
                        }

                        "EXPUNGE_ON_POLL" -> {
                            SelectOption(Expunge.EXPUNGE_ON_POLL.name) {
                                resources.stringResource(R.string.account_settings_expunge_policy_on_poll)
                            }
                        }

                        "EXPUNGE_MANUALLY" -> {
                            SelectOption(Expunge.EXPUNGE_MANUALLY.name) {
                                resources.stringResource(R.string.account_settings_expunge_policy_manual)
                            }
                        }

                        else -> {
                            error("Invalid message expungePolicy")
                        }
                    }
                    val maxFolderToCheckWithPush = when (it.maxPushFolders) {
                        5 -> {
                            SelectOption("5") {
                                resources.stringResource(R.string.account_settings_push_limit_5)
                            }
                        }

                        10 -> {
                            SelectOption("10") {
                                resources.stringResource(R.string.account_settings_push_limit_10)
                            }
                        }

                        25 -> {
                            SelectOption("25") {
                                resources.stringResource(R.string.account_settings_push_limit_25)
                            }
                        }

                        50 -> {
                            SelectOption("50") {
                                resources.stringResource(R.string.account_settings_push_limit_50)
                            }
                        }

                        100 -> {
                            SelectOption("100") {
                                resources.stringResource(R.string.account_settings_push_limit_100)
                            }
                        }

                        250 -> {
                            SelectOption("250") {
                                resources.stringResource(R.string.account_settings_push_limit_250)
                            }
                        }

                        500 -> {
                            SelectOption("500") {
                                resources.stringResource(R.string.account_settings_push_limit_500)
                            }
                        }

                        1000 -> {
                            SelectOption("1000") {
                                resources.stringResource(R.string.account_settings_push_limit_1000)
                            }
                        }

                        else -> {
                            error("Invalid push limit")
                        }
                    }
                    val refreshIdleConnection = when (it.idleRefreshMinutes) {
                        2 -> {
                            SelectOption("2") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_2min)
                            }
                        }

                        3 -> {
                            SelectOption("3") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_3min)
                            }
                        }

                        6 -> {
                            SelectOption("6") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_6min)
                            }
                        }

                        12 -> {
                            SelectOption("12") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_12min)
                            }
                        }

                        24 -> {
                            SelectOption("24") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_24min)
                            }
                        }

                        36 -> {
                            SelectOption("36") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_36min)
                            }
                        }

                        48 -> {
                            SelectOption("48") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_48min)
                            }
                        }

                        60 -> {
                            SelectOption("60") {
                                resources.stringResource(R.string.account_settings_idle_refresh_period_60min)
                            }
                        }

                        else -> {
                            error("Invalid idle refresh period")
                        }
                    }

                    updateState { state ->
                        state.copy(
                            localFolderSize = localFolderSize,
                            syncMessageFrom = syncMessageFrom,
                            fetchMessageUpTo = fetchMessageUpTo,
                            folderPollFrequency = folderPollFrequency,
                            syncServerDeletions = syncServerDeletions,
                            markAsReadWhenDeleted = markReadWhenDeleted,
                            whenIDeleteAMessage = whenIDeleteAMessage,
                            eraseDeletedMessageOnServer = eraseDeletedMessageOnServer,
                            maxFolderToCheckWithPush = maxFolderToCheckWithPush,
                            refreshIdleConnection = refreshIdleConnection,
                        )
                    }
                },
                onFailure = {
                    handleError(it)
                },
            )
        }
    }

    private fun observeAccountName() {
        getAccountName(accountId)
            .onEach { outcome ->
                outcome.handle(
                    onSuccess = { updateState { state -> state.copy(subtitle = it) } },
                    onFailure = { handleError(it) },
                )
            }.launchIn(viewModelScope)
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.StorageError -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.UnsupportedFormat -> logger.error(tag = TAG, message = { error.message })
        }
    }
}
