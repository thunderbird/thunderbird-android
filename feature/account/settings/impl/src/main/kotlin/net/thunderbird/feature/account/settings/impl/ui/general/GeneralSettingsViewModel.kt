package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateGeneralSettingCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

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
            is Event.OnSettingValueChange -> updateSetting(event.setting)
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
                    onSuccess = { settings ->
                        updateState { state ->
                            state.copy(
                                settings = settings,
                            )
                        }
                    },
                    onFailure = { handleError(it) },
                )
            }.launchIn(viewModelScope)
    }

    private fun updateSetting(setting: SettingValue<*>) {
        val (id, value) = setting.let { it.id to it.value }

        viewModelScope.launch {
            val command = when (id) {
                GeneralPreference.COLOR.generateId(accountId) -> {
                    UpdateGeneralSettingCommand.UpdateColor(value as Int)
                }
                GeneralPreference.NAME.generateId(accountId) -> {
                    UpdateGeneralSettingCommand.UpdateName(value as String)
                }
                GeneralPreference.PROFILE_INDICATOR.generateId(accountId) -> {
                    UpdateGeneralSettingCommand.UpdateAvatar(value as AccountAvatar)
                }
                else -> null
            }
            command?.let { updateGeneralSettings(accountId, it) }
        }
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> Log.w(error.message)
        }
    }
}
