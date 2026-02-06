package net.thunderbird.feature.account.settings.impl.ui.readingMail

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.State

private const val TAG = "ReadingMailSettingsViewModel"
internal class ReadingMailSettingsViewModel(
    private val accountId: AccountId,
    private val getAccountName: UseCase.GetAccountName,
    private val getLegacyAccount: UseCase.GetLegacyAccount,
    private val updateReadMailSettings: UseCase.UpdateReadMailSettings,
    private val logger: Logger,
    private val resources: StringsResourceManager,
    initialState: State = State(
        showPictures = SelectOption(
            ShowPictures.NEVER.name,
        ) {
            resources.stringResource(R.string.account_settings_show_pictures_never)
        },
    ),
) : BaseViewModel<State, Event, Effect>(initialState), ReadingMailSettingsContract.ViewModel {
    override fun event(event: Event) {
        when (event) {
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)
            is Event.OnShowPicturesChange -> {
                viewModelScope.launch {
                    updateReadMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract.UpdateReadMessageSettingsCommand.UpdateShowPictures(
                            event.showPictures.id,
                        ),
                    ).handle(
                        onSuccess = {
                            updateState { state -> state.copy(showPictures = event.showPictures) }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }
            }
            is Event.OnIsMarkMessageAsReadOnViewToggle -> {
                viewModelScope.launch {
                    updateReadMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract.UpdateReadMessageSettingsCommand
                            .UpdateIsMarkMessageAsReadOnView(
                                event.isMarkMessageAsReadOnView,
                            ),
                    ).handle(
                        onSuccess = {
                            updateState { state ->
                                state.copy(
                                    isMarkMessageAsReadOnView = event.isMarkMessageAsReadOnView,
                                )
                            }
                        },
                        onFailure = {
                            handleError(it)
                        },
                    )
                }

                updateState { state ->
                    state.copy(isMarkMessageAsReadOnView = event.isMarkMessageAsReadOnView)
                }
            }
        }
    }

    init {
        observeAccountName()
        observeReadingMailSettings()
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

    private fun observeReadingMailSettings() {
        viewModelScope.launch {
            getLegacyAccount(accountId).handle(
                onSuccess = {
                    val showPictures = when (it.showPictures.name) {
                        ShowPictures.NEVER.name -> {
                            SelectOption(
                                ShowPictures.NEVER.name,
                            ) {
                                resources.stringResource(R.string.account_settings_show_pictures_never)
                            }
                        }

                        ShowPictures.ALWAYS.name -> {
                            SelectOption(
                                ShowPictures.ALWAYS.name,
                            ) {
                                resources.stringResource(R.string.account_settings_show_pictures_always)
                            }
                        }

                        ShowPictures.ONLY_FROM_CONTACTS.name -> {
                            SelectOption(
                                ShowPictures.ONLY_FROM_CONTACTS.name,
                            ) {
                                resources.stringResource(R.string.account_settings_show_pictures_only_from_contacts)
                            }
                        }

                        else -> {
                            error("Invalid showPictures value: ${it.showPictures.name}")
                        }
                    }

                    updateState { state ->
                        state.copy(
                            showPictures = showPictures,
                            isMarkMessageAsReadOnView = it.isMarkMessageAsReadOnView,
                        )
                    }
                },
                onFailure = {
                    handleError(it)
                },
            )
        }
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.StorageError -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.UnsupportedFormat -> logger.error(tag = TAG, message = { error.message })
        }
    }
}
