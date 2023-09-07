package app.k9mail.feature.account.setup.ui.options

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount

interface AccountOptionsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val accountName: StringInputField = StringInputField(),
        val displayName: StringInputField = StringInputField(),
        val emailSignature: StringInputField = StringInputField(),
        val checkFrequency: EmailCheckFrequency = EmailCheckFrequency.DEFAULT,
        val messageDisplayCount: EmailDisplayCount = EmailDisplayCount.DEFAULT,
        val showNotification: Boolean = false,
    )

    sealed interface Event {
        data class OnAccountNameChanged(val accountName: String) : Event
        data class OnDisplayNameChanged(val displayName: String) : Event
        data class OnEmailSignatureChanged(val emailSignature: String) : Event
        data class OnCheckFrequencyChanged(val checkFrequency: EmailCheckFrequency) : Event
        data class OnMessageDisplayCountChanged(val messageDisplayCount: EmailDisplayCount) : Event
        data class OnShowNotificationChanged(val showNotification: Boolean) : Event

        object LoadAccountState : Event

        object OnNextClicked : Event
        object OnBackClicked : Event
    }

    sealed interface Effect {
        object NavigateNext : Effect
        object NavigateBack : Effect
    }

    interface Validator {
        fun validateAccountName(accountName: String): ValidationResult
        fun validateDisplayName(displayName: String): ValidationResult
        fun validateEmailSignature(emailSignature: String): ValidationResult
    }
}
