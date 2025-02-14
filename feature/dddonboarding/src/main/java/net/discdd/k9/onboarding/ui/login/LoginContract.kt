package net.discdd.k9.onboarding.ui.login

import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.domain.entity.AccountUuid

interface LoginContract {
    //interface ViewModel: UnidirectionalViewModel<State, Event, Effect> {}

    data class State(
        val emailAddress: StringInputField = StringInputField(),
        val password: StringInputField = StringInputField(),
    )

    sealed interface Event {
        data class EmailAddressChanged(val emailAddress: String): Event
        data class PasswordChanged(val password: String): Event
        data class OnClickLogin(val emailAddress: String, val password: String): Event

        data object CheckAuthState: Event
    }

    sealed interface Effect {
        data object OnPendingState: Effect
        data class OnLoggedInState(val  accountUuid: AccountUuid): Effect
    }
}
