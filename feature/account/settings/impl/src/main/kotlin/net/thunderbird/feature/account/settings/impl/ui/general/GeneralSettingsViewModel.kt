package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
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
        viewModelScope.launch {
            getAccountName(accountId).collect { outcome ->
                outcome.handle(
                    onSuccess = { accountName ->
                        updateState { state ->
                            state.copy(
                                subtitle = accountName,
                            )
                        }
                    },
                    onFailure = { handleError(it) },
                )
            }
        }

        viewModelScope.launch {
            getGeneralSettings(accountId).collect { outcome ->
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
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnSettingValueChange -> updatePreference(event.setting)
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)
        }
    }

    private fun updatePreference(setting: SettingValue<*>) {
        viewModelScope.launch {
            updateGeneralSettings(accountId, setting)
        }
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> Log.w(error.message)
        }
    }
}
