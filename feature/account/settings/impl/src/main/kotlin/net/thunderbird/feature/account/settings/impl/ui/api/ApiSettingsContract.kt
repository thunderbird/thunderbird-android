package net.thunderbird.feature.account.settings.impl.ui.api

import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.validation.input.StringInputField

internal interface ApiSettingsContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val name: StringInputField = StringInputField(),
        val emailId: String = "",
        val clientId: String = "",
        val requiresClientId: Boolean = false,
        val isLoading: Boolean = true,
        val isApiAccount: Boolean = false,
        val isGraphAccount: Boolean = false,
    )

    sealed interface Event {
        data class OnNameChange(val name: String) : Event
        data class OnClientIdChange(val clientId: String) : Event
        data object OnReauthorizeClick : Event
        data object OnBackPressed : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data object ReauthorizeAccount : Effect
    }
}
