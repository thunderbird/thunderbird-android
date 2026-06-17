package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import com.fsck.k9.mail.MailProxyType
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.input.BooleanInputField
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField

interface AccountAutoDiscoveryContract {

    enum class ConfigStep {
        EMAIL_ADDRESS,
        OAUTH,
        PASSWORD,
        MANUAL_SETUP,
    }

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val oAuthViewModel: AccountOAuthContract.ViewModel

        fun initState(state: State)
    }

    data class State(
        val configStep: ConfigStep = ConfigStep.EMAIL_ADDRESS,
        val emailAddress: StringInputField = StringInputField(),
        val password: StringInputField = StringInputField(),
        val proxyType: MailProxyType = MailProxyType.USE_GLOBAL,
        val proxyServer: StringInputField = StringInputField(),
        val proxyPort: NumberInputField = NumberInputField(),
        val proxyDns: Boolean = true,
        val proxyUsername: StringInputField = StringInputField(),
        val proxyPassword: StringInputField = StringInputField(),
        val isPrivateKeyboardEnabled: Boolean = true,
        val autoDiscoverySettings: AutoDiscoveryResult.Settings? = null,
        val configurationApproved: BooleanInputField = BooleanInputField(),
        val authorizationState: AuthorizationState? = null,

        val isSuccess: Boolean = false,
        override val error: Error? = null,
        override val isLoading: Boolean = false,

        val isNextButtonVisible: Boolean = true,
        val isNetworkSettingsExpanded: Boolean = false,
    ) : LoadingErrorState<Error>

    sealed interface Event {
        data class EmailAddressChanged(val emailAddress: String) : Event
        data class PasswordChanged(val password: String) : Event
        data class ProxyTypeChanged(val proxyType: MailProxyType) : Event
        data class ProxyServerChanged(val proxyServer: String) : Event
        data class ProxyPortChanged(val proxyPort: Long?) : Event
        data class ProxyDnsChanged(val proxyDns: Boolean) : Event
        data class ProxyUsernameChanged(val proxyUsername: String) : Event
        data class ProxyPasswordChanged(val proxyPassword: String) : Event
        data class ResultApprovalChanged(val confirmed: Boolean) : Event
        data class OnOAuthResult(val result: OAuthResult) : Event

        data object OnNextClicked : Event
        data object OnBackClicked : Event
        data object OnRetryClicked : Event
        data object OnEditConfigurationClicked : Event
        data object OnManualSetupClicked : Event
        data object NetworkSettingsToggled : Event
    }

    sealed class Effect {
        data class NavigateNext(
            val result: AutoDiscoveryUiResult,
        ) : Effect()

        data object NavigateBack : Effect()
    }

    interface Validator {
        fun validateEmailAddress(emailAddress: String): ValidationOutcome
        fun validatePassword(password: String): ValidationOutcome
        fun validateProxyServer(server: String): ValidationOutcome
        fun validateProxyPort(port: Long?): ValidationOutcome
        fun validateConfigurationApproval(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationOutcome
    }

    sealed interface Error {
        data object NetworkError : Error
        data object UnknownError : Error
    }

    data class AutoDiscoveryUiResult(
        val isAutomaticConfig: Boolean,
        val incomingProtocolType: IncomingProtocolType?,
    )
}
