package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.input.StringInputField

interface DisplayOptionsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val accountName: StringInputField = StringInputField(),
        val displayName: StringInputField = StringInputField(),
        val emailSignature: StringInputField = StringInputField(),
    )

    sealed interface Event {
        data class OnAccountNameChanged(val accountName: String) : Event
        data class OnDisplayNameChanged(val displayName: String) : Event
        data class OnEmailSignatureChanged(val emailSignature: String) : Event

        data object LoadAccountState : Event

        data object OnNextClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateNext : Effect
        data object NavigateBack : Effect
    }

    interface Validator {
        fun validateAccountName(accountName: String): ValidationOutcome
        fun validateDisplayName(displayName: String): ValidationOutcome
        fun validateEmailSignature(emailSignature: String): ValidationOutcome
    }
}
