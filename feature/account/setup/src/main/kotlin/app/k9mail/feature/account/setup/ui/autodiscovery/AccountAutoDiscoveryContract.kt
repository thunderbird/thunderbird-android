package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.input.BooleanInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract

interface AccountAutoDiscoveryContract {

    enum class ConfigStep {
        EMAIL_ADDRESS,
        OAUTH,
        PASSWORD,
    }

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val oAuthViewModel: AccountOAuthContract.ViewModel

        fun initState(state: State)
    }

    data class State(
        val configStep: ConfigStep = ConfigStep.EMAIL_ADDRESS,
        val emailAddress: StringInputField = StringInputField(),
        val password: StringInputField = StringInputField(),
        val autoDiscoverySettings: AutoDiscoveryResult.Settings? = null,
        val configurationApproved: BooleanInputField = BooleanInputField(),
        val authorizationState: AuthorizationState? = null,

        val isSuccess: Boolean = false,
        val error: Error? = null,
        val isLoading: Boolean = false,

        val isNextButtonVisible: Boolean = true,
    )

    sealed interface Event {
        data class EmailAddressChanged(val emailAddress: String) : Event
        data class PasswordChanged(val password: String) : Event
        data class ResultApprovalChanged(val confirmed: Boolean) : Event
        data class OnOAuthResult(val result: OAuthResult) : Event

        object OnNextClicked : Event
        object OnBackClicked : Event
        object OnRetryClicked : Event
        object OnEditConfigurationClicked : Event
    }

    sealed class Effect {
        data class NavigateNext(val isAutomaticConfig: Boolean) : Effect()

        object NavigateBack : Effect()
    }

    interface Validator {
        fun validateEmailAddress(emailAddress: String): ValidationResult
        fun validatePassword(password: String): ValidationResult
        fun validateConfigurationApproval(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationResult
    }

    sealed interface Error {
        object NetworkError : Error
        object UnknownError : Error
    }
}
