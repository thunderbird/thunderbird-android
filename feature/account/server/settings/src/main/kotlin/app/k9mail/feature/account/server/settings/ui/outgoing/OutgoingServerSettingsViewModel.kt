package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Validator
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.ViewModel
import net.thunderbird.core.outcome.Outcome

open class OutgoingServerSettingsViewModel(
    initialState: State = State(),
    override val mode: InteractionMode,
    private val validator: Validator,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
) : BaseViewModel<State, Event, Effect>(initialState = initialState), ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountState -> handleOneTimeEvent(event, ::loadAccountState)

            is Event.ServerChanged -> updateState { it.copy(server = it.server.updateValue(event.server)) }
            is Event.SecurityChanged -> updateSecurity(event.security)
            is Event.PortChanged -> updateState { it.copy(port = it.port.updateValue(event.port)) }
            is Event.AuthenticationTypeChanged -> updateState { it.copy(authenticationType = event.authenticationType) }
            is Event.UsernameChanged -> updateState { it.copy(username = it.username.updateValue(event.username)) }
            is Event.PasswordChanged -> updateState { it.copy(password = it.password.updateValue(event.password)) }
            is Event.ClientCertificateChanged -> updateState {
                it.copy(clientCertificateAlias = event.clientCertificateAlias)
            }

            Event.OnNextClicked -> onNext()
            Event.OnBackClicked -> onBack()
        }
    }

    protected open fun loadAccountState() {
        updateState {
            accountStateRepository.getState().toOutgoingServerSettingsState()
        }
    }

    private fun onNext() {
        submitConfig()
    }

    private fun updateSecurity(security: ConnectionSecurity) {
        updateState {
            it.copy(
                security = security,
                port = it.port.updateValue(security.toSmtpDefaultPort()),
            )
        }
    }

    private fun submitConfig() = with(state.value) {
        val serverResult = validator.validateServer(server.value)
        val portResult = validator.validatePort(port.value)
        val usernameResult = validator.validateUsername(username.value)
        val passwordResult = if (authenticationType.isPasswordRequired) {
            validator.validatePassword(password.value)
        } else {
            Outcome.Success(Unit)
        }

        val hasError = listOf(serverResult, portResult, usernameResult, passwordResult)
            .any { it is Outcome.Failure }

        updateState {
            it.copy(
                server = it.server.updateFromValidationOutcome(serverResult),
                port = it.port.updateFromValidationOutcome(portResult),
                username = it.username.updateFromValidationOutcome(usernameResult),
                password = it.password.updateFromValidationOutcome(passwordResult),
            )
        }

        if (!hasError) {
            accountStateRepository.setOutgoingServerSettings(state.value.toServerSettings())
            navigateNext()
        }
    }

    private fun onBack() {
        navigateBack()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
