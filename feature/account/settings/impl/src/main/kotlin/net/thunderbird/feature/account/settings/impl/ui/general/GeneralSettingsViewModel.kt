package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.coroutines.launch
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State
import timber.log.Timber

internal class GeneralSettingsViewModel(
    private val accountId: AccountId,
    private val getAccountName: UseCase.GetAccountName,
    private val getGeneralPreferences: UseCase.GetGeneralPreferences,
    private val updateGeneralPreferences: UseCase.UpdateGeneralPreferences,
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
            getGeneralPreferences(accountId).collect { outcome ->
                outcome.handle(
                    onSuccess = { preferences ->
                        updateState { state ->
                            state.copy(
                                preferences = preferences,
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
            is Event.OnPreferenceSettingChange -> updatePreference(event.preference)
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)
        }
    }

    private fun updatePreference(preference: PreferenceSetting<*>) {
        viewModelScope.launch {
            updateGeneralPreferences(accountId, preference)
        }
    }

    private fun handleError(error: SettingsError) {
        when (error) {
            is SettingsError.NotFound -> Timber.w(error.message)
        }
    }
}
