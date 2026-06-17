package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.lifecycle.viewModelScope
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.demo.DemoServerSettings
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.entity.AutoDiscoveryAuthenticationType
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.AutoDiscoveryUiResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Error
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Validator
import com.fsck.k9.mail.MailProxySettings
import com.fsck.k9.mail.MailProxyType
import kotlinx.coroutines.launch
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.core.validation.input.StringInputField

@Suppress("TooManyFunctions")
internal class AccountAutoDiscoveryViewModel(
    initialState: State = State(),
    private val validator: Validator,
    private val getAutoDiscovery: UseCase.GetAutoDiscovery,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    override val oAuthViewModel: AccountOAuthContract.ViewModel,
    generalSettingsManager: GeneralSettingsManager? = null,
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState.copy(
        isPrivateKeyboardEnabled = generalSettingsManager?.getConfig()?.privacy?.isPrivateKeyboardEnabled ?: true,
    ),
),
    AccountAutoDiscoveryContract.ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.EmailAddressChanged -> changeEmailAddress(event.emailAddress)

            is Event.PasswordChanged -> changePassword(event.password)

            is Event.ProxyTypeChanged -> updateState { it.copy(proxyType = event.proxyType) }

            is Event.ProxyServerChanged -> updateState {
                it.copy(proxyServer = it.proxyServer.updateValue(event.proxyServer))
            }

            is Event.ProxyPortChanged -> updateState { it.copy(proxyPort = it.proxyPort.updateValue(event.proxyPort)) }

            is Event.ProxyDnsChanged -> updateState { it.copy(proxyDns = event.proxyDns) }

            is Event.ProxyUsernameChanged -> updateState {
                it.copy(proxyUsername = it.proxyUsername.updateValue(event.proxyUsername))
            }

            is Event.ProxyPasswordChanged -> updateState {
                it.copy(proxyPassword = it.proxyPassword.updateValue(event.proxyPassword))
            }

            is Event.ResultApprovalChanged -> changeConfigurationApproval(event.confirmed)

            is Event.OnOAuthResult -> onOAuthResult(event.result)

            Event.OnNextClicked -> onNext()

            Event.OnBackClicked -> onBack()

            Event.OnRetryClicked -> onRetry()

            Event.OnEditConfigurationClicked -> {
                navigateNext(isAutomaticConfig = false)
            }

            Event.OnManualSetupClicked -> submitManualSetup()

            Event.NetworkSettingsToggled -> toggleNetworkSettings()
        }
    }

    private fun toggleNetworkSettings() {
        updateState {
            it.copy(isNetworkSettingsExpanded = !it.isNetworkSettingsExpanded)
        }
    }

    private fun changeEmailAddress(emailAddress: String) {
        accountStateRepository.clear()
        updateState {
            State(
                emailAddress = StringInputField(value = emailAddress),
                isNextButtonVisible = true,
            )
        }
    }

    private fun changePassword(password: String) {
        updateState {
            it.copy(
                password = it.password.updateValue(password),
            )
        }
    }

    private fun changeConfigurationApproval(approved: Boolean) {
        updateState {
            it.copy(
                configurationApproved = it.configurationApproved.updateValue(approved),
            )
        }
    }

    private fun onNext() {
        when (state.value.configStep) {
            ConfigStep.EMAIL_ADDRESS ->
                if (state.value.error != null) {
                    updateState {
                        it.copy(
                            error = null,
                            configStep = ConfigStep.PASSWORD,
                        )
                    }
                } else {
                    submitEmail()
                }

            ConfigStep.PASSWORD -> submitPassword()

            ConfigStep.OAUTH -> Unit

            ConfigStep.MANUAL_SETUP -> navigateNext(isAutomaticConfig = false)
        }
    }

    private fun onRetry() {
        updateState {
            it.copy(error = null)
        }
        loadAutoDiscovery()
    }

    private fun submitEmail() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val proxyServerValidationResult = validateProxyServer()
            val proxyPortValidationResult = validateProxyPort()
            val hasError = listOf(
                emailValidationResult,
                proxyServerValidationResult,
                proxyPortValidationResult,
            ).any { it is Outcome.Failure }

            val hasProxyValidationError = proxyServerValidationResult is Outcome.Failure ||
                proxyPortValidationResult is Outcome.Failure

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationOutcome(emailValidationResult),
                    proxyServer = it.proxyServer.updateFromValidationOutcome(proxyServerValidationResult),
                    proxyPort = it.proxyPort.updateFromValidationOutcome(proxyPortValidationResult),
                    isNetworkSettingsExpanded = it.isNetworkSettingsExpanded || hasProxyValidationError,
                )
            }

            if (!hasError) {
                loadAutoDiscovery()
            }
        }
    }

    private fun submitManualSetup() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val proxyServerValidationResult = validateProxyServer()
            val proxyPortValidationResult = validateProxyPort()
            val hasError = listOf(
                emailValidationResult,
                proxyServerValidationResult,
                proxyPortValidationResult,
            ).any { it is Outcome.Failure }

            val hasProxyValidationError = proxyServerValidationResult is Outcome.Failure ||
                proxyPortValidationResult is Outcome.Failure

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationOutcome(emailValidationResult),
                    proxyServer = it.proxyServer.updateFromValidationOutcome(proxyServerValidationResult),
                    proxyPort = it.proxyPort.updateFromValidationOutcome(proxyPortValidationResult),
                    isNetworkSettingsExpanded = it.isNetworkSettingsExpanded || hasProxyValidationError,
                )
            }

            if (!hasError) {
                navigateNext(isAutomaticConfig = false)
            }
        }
    }

    private fun loadAutoDiscovery() {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isLoading = true,
                )
            }

            val result = getAutoDiscovery.execute(state.value.emailAddress.value, state.value.toProxySettings())
            when (result) {
                AutoDiscoveryResult.NoUsableSettingsFound -> updateNoSettingsFound()
                is AutoDiscoveryResult.Settings -> updateAutoDiscoverySettings(result)
                is AutoDiscoveryResult.NetworkError -> updateError(Error.NetworkError)
                is AutoDiscoveryResult.UnexpectedException -> updateError(Error.UnknownError)
            }
        }
    }

    private fun updateNoSettingsFound() {
        updateState {
            it.copy(
                isLoading = false,
                autoDiscoverySettings = null,
                configStep = ConfigStep.MANUAL_SETUP,
            )
        }
    }

    private fun updateAutoDiscoverySettings(settings: AutoDiscoveryResult.Settings) {
        if (settings.incomingServerSettings is DemoServerSettings) {
            updateState {
                it.copy(
                    isLoading = false,
                    autoDiscoverySettings = settings,
                    configStep = ConfigStep.PASSWORD,
                    isNextButtonVisible = true,
                )
            }
            return
        }

        val imapServerSettings = settings.incomingServerSettings as ImapServerSettings
        val isOAuth = imapServerSettings.authenticationTypes.first() == AutoDiscoveryAuthenticationType.OAuth2

        if (isOAuth) {
            oAuthViewModel.initState(
                AccountOAuthContract.State(
                    hostname = imapServerSettings.hostname.value,
                    emailAddress = state.value.emailAddress.value,
                ),
            )
        }

        updateState {
            it.copy(
                isLoading = false,
                autoDiscoverySettings = settings,
                configStep = if (isOAuth) ConfigStep.OAUTH else ConfigStep.PASSWORD,
                isNextButtonVisible = !isOAuth,
            )
        }
    }

    private fun updateError(error: Error) {
        updateState {
            it.copy(
                isLoading = false,
                error = error,
            )
        }
    }

    private fun submitPassword() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val passwordValidationResult = validator.validatePassword(password.value)
            val configurationApprovalValidationResult = validator.validateConfigurationApproval(
                isApproved = configurationApproved.value,
                isAutoDiscoveryTrusted = autoDiscoverySettings?.isTrusted,
            )
            val hasError = listOf(
                emailValidationResult,
                passwordValidationResult,
                configurationApprovalValidationResult,
            ).any { it is Outcome.Failure }

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationOutcome(emailValidationResult),
                    password = it.password.updateFromValidationOutcome(passwordValidationResult),
                    configurationApproved = it.configurationApproved.updateFromValidationOutcome(
                        configurationApprovalValidationResult,
                    ),
                )
            }

            if (!hasError) {
                navigateNext(state.value.autoDiscoverySettings != null)
            }
        }
    }

    private fun onBack() {
        when (state.value.configStep) {
            ConfigStep.EMAIL_ADDRESS -> {
                if (state.value.error != null) {
                    updateState {
                        it.copy(error = null)
                    }
                } else {
                    navigateBack()
                }
            }

            ConfigStep.OAUTH,
            ConfigStep.PASSWORD,
            ConfigStep.MANUAL_SETUP,
            -> updateState {
                it.copy(
                    configStep = ConfigStep.EMAIL_ADDRESS,
                    password = StringInputField(),
                    isNextButtonVisible = true,
                )
            }
        }
    }

    private fun onOAuthResult(result: OAuthResult) {
        if (result is OAuthResult.Success) {
            updateState {
                it.copy(authorizationState = result.authorizationState)
            }

            navigateNext(isAutomaticConfig = true)
        } else {
            updateState {
                it.copy(authorizationState = null)
            }
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun State.validateProxyServer() = if (proxyType.isProxyValidationRequired) {
        validator.validateProxyServer(proxyServer.value)
    } else {
        ValidationSuccess
    }

    private fun State.validateProxyPort() = if (proxyType.isProxyValidationRequired) {
        validator.validateProxyPort(proxyPort.value)
    } else {
        ValidationSuccess
    }

    private fun navigateNext(isAutomaticConfig: Boolean) {
        accountStateRepository.setState(state.value.toAccountState())

        emitEffect(
            Effect.NavigateNext(
                result = mapToAutoDiscoveryResult(
                    isAutomaticConfig = isAutomaticConfig,
                    incomingServerSettings = state.value.autoDiscoverySettings?.incomingServerSettings,
                ),
            ),
        )
    }

    private fun mapToAutoDiscoveryResult(
        isAutomaticConfig: Boolean,
        incomingServerSettings: IncomingServerSettings?,
    ): AutoDiscoveryUiResult {
        val incomingProtocolType = if (incomingServerSettings is ImapServerSettings) {
            IncomingProtocolType.IMAP
        } else {
            null
        }

        return AutoDiscoveryUiResult(
            isAutomaticConfig = isAutomaticConfig,
            incomingProtocolType = incomingProtocolType,
        )
    }
}

internal fun State.toProxySettings(): MailProxySettings {
    return when (proxyType) {
        MailProxyType.USE_GLOBAL -> MailProxySettings.USE_GLOBAL

        MailProxyType.NONE -> MailProxySettings.NONE

        else -> MailProxySettings(
            type = proxyType,
            host = proxyServer.value.trim(),
            port = proxyPort.value!!.toInt(),
            proxyDns = proxyDns,
            username = proxyUsername.value.trim().ifBlank { null },
            password = proxyPassword.value.ifBlank { null },
        )
    }
}

private val MailProxyType.isProxyValidationRequired: Boolean
    get() = this != MailProxyType.USE_GLOBAL && this != MailProxyType.NONE
