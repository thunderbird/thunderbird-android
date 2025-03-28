package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.coroutines.launch
import net.thunderbird.core.outcome.handle
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

internal class GeneralSettingsViewModel(
    private val accountId: AccountId,
    private val getGeneralPreferences: UseCase.GetGeneralPreferences,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), GeneralSettingsContract.ViewModel {

    init {
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
                    onFailure = {},
                )
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)
        }
    }
}
