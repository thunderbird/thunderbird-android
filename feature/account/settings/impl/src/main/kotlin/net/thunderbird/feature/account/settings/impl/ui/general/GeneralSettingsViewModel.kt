package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateGeneralSettingCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

@Suppress("TooManyFunctions")
internal class GeneralSettingsViewModel(
    private val accountId: AccountId,
    private val getAccountName: UseCase.GetAccountName,
    private val getGeneralSettings: UseCase.GetGeneralSettings,
    private val updateGeneralSettings: UseCase.UpdateGeneralSettings,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), GeneralSettingsContract.ViewModel {

    init {
        observeAccountName()
        observeGeneralSettings()
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)
            is Event.OnAvatarChange -> updateSetting(UpdateGeneralSettingCommand.UpdateAvatar(event.avatar))
            is Event.OnColorChange -> updateSetting(UpdateGeneralSettingCommand.UpdateColor(event.color))
            is Event.OnNameChange -> updateSetting(UpdateGeneralSettingCommand.UpdateName(event.name))
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

    private fun observeGeneralSettings() {
        getGeneralSettings(accountId)
            .onEach { outcome ->
                outcome.handle(
                    onSuccess = { profile ->
                        updateState { state ->
                            state.copy(
                                name = state.name.updateValue(profile.name),
                                color = IntegerInputField(value = profile.color),
                                avatar = profile.avatar,
                            )
                        }
                    },
                    onFailure = { handleError(it) },
                )
            }.launchIn(viewModelScope)
    }

    private fun updateSetting(command: UpdateGeneralSettingCommand) {
        viewModelScope.launch {
            updateGeneralSettings(accountId, command)
        }
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> Log.w(error.message)
        }
    }
}
