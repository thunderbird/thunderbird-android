package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.outcome.handleAsync
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.R
import net.thunderbird.feature.mail.message.list.domain.CreateArchiveFolderOutcome
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.SetAccountFolderOutcome
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.Effect
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.Event
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.State
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.ViewModel

internal class SetupArchiveFolderDialogViewModel(
    private val accountUuid: String,
    private val logger: Logger,
    private val getAccountFolders: DomainContract.UseCase.GetAccountFolders,
    private val createArchiveFolder: DomainContract.UseCase.CreateArchiveFolder,
    private val setArchiveFolder: DomainContract.UseCase.SetArchiveFolder,
    private val resourceManager: StringsResourceManager,
    private val generalSettingsManager: GeneralSettingsManager,
) : BaseViewModel<State, Event, Effect>(
    initialState = if (generalSettingsManager.getSettings().shouldShowSetupArchiveFolderDialog) {
        State.EmailCantBeArchived()
    } else {
        State.Closed(isDoNotShowDialogAgainChecked = true)
    },
),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.MoveNext -> onNext(state = state.value)

            Event.OnDoneClicked -> onDoneClicked(state = state.value)

            Event.OnDismissClicked -> onDismissClicked()

            is Event.OnDoNotShowDialogAgainChanged -> onDoNotShowDialogAgainChanged(isChecked = event.isChecked)

            is Event.OnCreateFolderClicked -> onCreateFolderClicked(newFolderName = event.newFolderName)

            is Event.OnFolderSelected -> onFolderSelected(folder = event.folder)
        }
    }

    private fun onNext(state: State) {
        when (state) {
            is State.ChooseArchiveFolder -> updateState {
                State.CreateArchiveFolder(folderName = "")
            }

            is State.EmailCantBeArchived -> {
                updateState { State.ChooseArchiveFolder(isLoadingFolders = true) }
                viewModelScope.launch {
                    getAccountFolders(accountUuid = accountUuid).handle(
                        onSuccess = { folders ->
                            updateState {
                                State.ChooseArchiveFolder(
                                    isLoadingFolders = false,
                                    folders = folders,
                                )
                            }
                        },
                        onFailure = { error ->
                            updateState {
                                State.ChooseArchiveFolder(
                                    isLoadingFolders = false,
                                    errorMessage = error.exception.message,
                                )
                            }
                        },
                    )
                }
            }

            else -> error("The '$state' state doesn't support the MoveNext event")
        }
    }

    private fun onDoneClicked(state: State) {
        check(state is State.ChooseArchiveFolder) { "The '$state' state doesn't support the OnDoneClicked event" }
        checkNotNull(state.selectedFolder) {
            "The selected folder is null. This should not happen."
        }

        viewModelScope.launch {
            setArchiveFolder(accountUuid = accountUuid, folder = state.selectedFolder).handle(
                onSuccess = {
                    updateState { State.Closed() }
                    emitEffect(Effect.DismissDialog)
                },
                onFailure = { error ->
                    updateState {
                        when (error) {
                            SetAccountFolderOutcome.Error.AccountNotFound ->
                                state.copy(
                                    errorMessage = resourceManager.stringResource(
                                        R.string.setup_archive_folder_set_archive_error_account_not_found,
                                        accountUuid,
                                    ),
                                )

                            is SetAccountFolderOutcome.Error.UnhandledError -> state.copy(
                                errorMessage = resourceManager.stringResource(
                                    R.string.setup_archive_folder_unhandled_error,
                                    error.throwable.message,
                                ),
                            )
                        }
                    }
                },
            )
        }
    }

    private fun onDismissClicked() {
        viewModelScope.launch {
            generalSettingsManager.setSetupArchiveShouldNotShowAgain(state.value.isDoNotShowDialogAgainChecked.not())
            updateState { State.Closed() }

            emitEffect(Effect.DismissDialog)
        }
    }

    private fun onDoNotShowDialogAgainChanged(isChecked: Boolean) {
        updateState { state ->
            when (state) {
                is State.EmailCantBeArchived -> state.copy(
                    isDoNotShowDialogAgainChecked = isChecked,
                )

                else -> state
            }
        }
    }

    private fun onCreateFolderClicked(newFolderName: String) {
        updateState { state ->
            when (state) {
                is State.CreateArchiveFolder -> state.copy(
                    folderName = newFolderName,
                    syncingMessage = resourceManager.stringResource(
                        R.string.setup_archive_folder_create_archive_folder_syncing,
                    ),
                    errorMessage = null,
                )

                else -> state
            }
        }

        createArchiveFolder(accountUuid = accountUuid, folderName = newFolderName)
            .onEach { outcome ->
                outcome.handleAsync(
                    onSuccess = ::onCreateArchiveFolderSuccess,
                    onFailure = ::onCreateArchiveFolderError,
                )
            }
            .launchIn(viewModelScope)
    }

    private suspend fun onCreateArchiveFolderSuccess(event: CreateArchiveFolderOutcome.Success) {
        when (event) {
            CreateArchiveFolderOutcome.Success.LocalFolderCreated -> {
                updateState { state ->
                    when (state) {
                        is State.CreateArchiveFolder -> state.copy(
                            syncingMessage = resourceManager.stringResource(
                                R.string.setup_archive_folder_create_archive_folder_local_folder_created,
                            ),
                        )

                        else -> state
                    }
                }
                logger.debug { "Folder created" }
            }

            CreateArchiveFolderOutcome.Success.Created -> {
                updateState { state ->
                    when (state) {
                        is State.CreateArchiveFolder -> state.copy(
                            syncingMessage = resourceManager.stringResource(
                                R.string.setup_archive_folder_create_archive_folder_remote_folder_created,
                            ),
                        )

                        else -> state
                    }
                }
                delay(100.milliseconds)
                updateState { State.Closed() }
                emitEffect(Effect.DismissDialog)
                logger.debug { "Sync finished" }
            }

            is CreateArchiveFolderOutcome.Success.SyncStarted -> {
                updateState { state ->
                    when (state) {
                        is State.CreateArchiveFolder -> state.copy(
                            syncingMessage = resourceManager.stringResource(
                                R.string.setup_archive_folder_create_archive_folder_creating_folder_email_provider,
                            ),
                        )

                        else -> state
                    }
                }
                logger.debug { "Started sync for ${event.serverId}" }
            }

            CreateArchiveFolderOutcome.Success.UpdatingSpecialFolders ->
                updateState { state ->
                    when (state) {
                        is State.CreateArchiveFolder -> state.copy(
                            syncingMessage = resourceManager.stringResource(
                                R.string.setup_archive_folder_create_archive_folder_updating_special_folder_rules,
                            ),
                        )

                        else -> state
                    }
                }
        }
    }

    private fun onCreateArchiveFolderError(error: CreateArchiveFolderOutcome.Error) {
        val errorMessage = when (error) {
            CreateArchiveFolderOutcome.Error.AccountNotFound ->
                resourceManager.stringResource(
                    R.string.setup_archive_folder_create_archive_folder_account_not_found,
                    accountUuid,
                ).also {
                    logger.error { it }
                }

            is CreateArchiveFolderOutcome.Error.SyncError.Failed ->
                resourceManager.stringResource(
                    R.string.setup_archive_folder_create_archive_folder_failed_sync_folder,
                    error.serverId,
                    error.message,
                ).also {
                    logger.error(
                        throwable = error.exception,
                        message = { it },
                    )
                }

            is CreateArchiveFolderOutcome.Error.UnhandledError -> resourceManager.stringResource(
                R.string.setup_archive_folder_unhandled_error,
                error.throwable.message,
            ).also {
                logger.error(throwable = error.throwable, message = { it })
            }

            is CreateArchiveFolderOutcome.Error.InvalidFolderName -> when {
                error.folderName.isBlank() -> resourceManager.stringResource(
                    R.string.setup_archive_folder_create_archive_folder_error_folder_name_blank,
                )

                else -> resourceManager.stringResource(
                    R.string.setup_archive_folder_create_archive_folder_invalid_folder_name,
                    error.folderName,
                )
            }

            is CreateArchiveFolderOutcome.Error.LocalFolderCreationError -> resourceManager.stringResource(
                R.string.setup_archive_folder_create_archive_folder_failed_create_local_folder,
                error.folderName,
            )
        }

        updateState { state ->
            when (state) {
                is State.CreateArchiveFolder -> state.copy(
                    errorMessage = errorMessage,
                    syncingMessage = null,
                )

                else -> state
            }
        }
    }

    private fun onFolderSelected(folder: RemoteFolder) {
        updateState { state ->
            when (state) {
                is State.ChooseArchiveFolder -> state.copy(selectedFolder = folder)
                else -> state
            }
        }
    }
}
