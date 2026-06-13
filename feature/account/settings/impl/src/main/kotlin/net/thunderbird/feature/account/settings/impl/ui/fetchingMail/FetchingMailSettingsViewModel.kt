package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.AccountId
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
    private val getAccountName: UseCase.GetAccountName,
    private val getLegacyAccount: UseCase.GetLegacyAccount,
    private val updateFetchingMailSettings: UseCase.UpdateFetchingMailSettings,
    private val fetchingMailSettingsOptionsMapper: FetchingMailSettingsOptionsMapper,
    initialState: State = State(
        localFolderSize = fetchingMailSettingsOptionsMapper.defaultLocalFolderSizeOption(),
        syncMessageFrom = fetchingMailSettingsOptionsMapper.defaultSyncMessageFromOption(),
        fetchMessageUpTo = fetchingMailSettingsOptionsMapper.defaultFetchMessageUpToOption(),
        folderPollFrequency = fetchingMailSettingsOptionsMapper.defaultFolderPollFrequencyOption(),
        syncServerDeletions = false,
        markAsReadWhenDeleted = false,
        whenIDeleteAMessage = fetchingMailSettingsOptionsMapper.whenIDeleteAMessageOption(),
        eraseDeletedMessageOnServer = fetchingMailSettingsOptionsMapper.defaultEraseDeletedMessageOnServerOption(),
        maxFolderToCheckWithPush = fetchingMailSettingsOptionsMapper.defaultMaxFolderToCheckWithPushOption(),
        refreshIdleConnection = fetchingMailSettingsOptionsMapper.defaultRefreshIdleConnectionOption(),
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
                            .UpdateFetchingMailSettingsCommand.UpdateDeletePolicy(
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
                            .UpdateFetchingMailSettingsCommand.UpdateExpungePolicy(
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
                            .UpdateFetchingMailSettingsCommand.UpdateMaxPushFolders(
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
                            .UpdateFetchingMailSettingsCommand.UpdateIdleRefreshMinutes(
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
                    val localFolderSize = fetchingMailSettingsOptionsMapper.localFolderSizeOption(it.displayCount)
                    val fetchMessageUpTo =
                        fetchingMailSettingsOptionsMapper.fetchMessageUpToOption(it.maximumAutoDownloadMessageSize)
                    val syncMessageFrom =
                        fetchingMailSettingsOptionsMapper.syncMessageFromOption(it.maximumPolledMessageAge)
                    val folderPollFrequency =
                        fetchingMailSettingsOptionsMapper.folderPollFrequencyOption(it.automaticCheckIntervalMinutes)
                    val syncServerDeletions = it.isSyncRemoteDeletions
                    val markReadWhenDeleted = it.isMarkMessageAsReadOnDelete
                    val whenIDeleteAMessage =
                        fetchingMailSettingsOptionsMapper.whenIDeleteAMessageOption(it.deletePolicy.name)
                    val eraseDeletedMessageOnServer =
                        fetchingMailSettingsOptionsMapper.eraseDeletedMessageOnServerOption(it.expungePolicy.name)
                    val maxFolderToCheckWithPush =
                        fetchingMailSettingsOptionsMapper.maxFolderToCheckWithPushOption(it.maxPushFolders.toString())
                    val refreshIdleConnection =
                        fetchingMailSettingsOptionsMapper.refreshIdleConnectionOption(it.idleRefreshMinutes.toString())

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
