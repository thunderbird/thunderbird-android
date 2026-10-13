package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.domain.entity.isNativeRestApi
import app.k9mail.feature.account.common.domain.entity.toDefaultPort
import app.k9mail.feature.account.oauth.domain.NativeApiOAuthConfiguration
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import app.k9mail.feature.account.server.settings.ui.common.toInvalidEmailDomain
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Validator
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.ViewModel
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.core.validation.input.StringInputField

open class IncomingServerSettingsViewModel(
    initialState: State = State(),
    override val mode: InteractionMode,
    private val validator: Validator,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    private val oauthConfigurationProvider: OAuthConfigurationProvider? = null,
    private val applicationId: String = "",
) : BaseViewModel<State, Event, Effect>(initialState = initialState), ViewModel {

    @Suppress("CyclomaticComplexMethod")
    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountState -> handleOneTimeEvent(event, ::loadAccountState)

            is Event.ProtocolTypeChanged -> updateProtocolType(event.protocolType)

            is Event.ServerChanged -> updateState { it.copy(server = it.server.updateValue(event.server)) }

            is Event.SecurityChanged -> updateSecurity(event.security)

            is Event.PortChanged -> updateState { it.copy(port = it.port.updateValue(event.port)) }

            is Event.AuthenticationTypeChanged -> updateState { it.copy(authenticationType = event.authenticationType) }

            is Event.UsernameChanged -> updateState {
                it.copy(username = it.username.updateValue(event.username), zohoConfigurationError = false)
            }

            is Event.PasswordChanged -> updateState { it.copy(password = it.password.updateValue(event.password)) }

            is Event.ApiOAuthClientIdChanged -> updateState {
                it.copy(
                    apiOAuthClientId = it.apiOAuthClientId.updateValue(event.clientId),
                    apiConfigurationError = false,
                )
            }

            is Event.ApiOAuthRedirectUriChanged -> updateState {
                it.copy(
                    apiOAuthRedirectUri = it.apiOAuthRedirectUri.updateValue(event.redirectUri),
                    apiConfigurationError = false,
                )
            }

            is Event.ZohoClientIdChanged -> updateState {
                it.copy(zohoClientId = it.zohoClientId.updateValue(event.clientId), zohoConfigurationError = false)
            }

            is Event.ZohoRedirectUriChanged -> updateState {
                it.copy(
                    zohoRedirectUri = it.zohoRedirectUri.updateValue(event.redirectUri),
                    zohoConfigurationError = false,
                )
            }

            is Event.ZohoIncomingOnlyChanged -> updateState { it.copy(zohoIncomingOnly = event.enabled) }

            is Event.ClientCertificateChanged -> updateState {
                it.copy(clientCertificateAlias = event.clientCertificateAlias)
            }

            is Event.ImapAutoDetectNamespaceChanged -> updateState {
                it.copy(imapAutodetectNamespaceEnabled = event.enabled)
            }

            is Event.ImapPrefixChanged -> updateState {
                it.copy(imapPrefix = it.imapPrefix.updateValue(event.imapPrefix))
            }

            is Event.ImapUseCompressionChanged -> updateState { it.copy(imapUseCompression = event.useCompression) }

            is Event.ImapSendClientInfoChanged -> updateState { it.copy(imapSendClientInfo = event.sendClientInfo) }

            Event.OnNextClicked -> onNext()

            Event.OnBackClicked -> onBack()

            Event.ReauthorizeMicrosoftAccountClicked -> {
                if (mode == InteractionMode.Edit && state.value.isGraphReauthorizationAvailable) {
                    emitEffect(Effect.ReauthorizeMicrosoftAccount)
                }
            }

            Event.ReauthorizeZohoAccountClicked -> {
                if (mode == InteractionMode.Edit && state.value.protocolType == IncomingProtocolType.ZOHO_MAIL_API) {
                    submitConfig(forceOAuth = true)
                }
            }
        }
    }

    protected open fun loadAccountState() {
        updateState {
            accountStateRepository.getState().toIncomingServerSettingsState()
        }
    }

    private fun onNext() {
        submitConfig()
    }

    private fun updateProtocolType(protocolType: IncomingProtocolType) {
        updateState {
            val accountState = accountStateRepository.getState()
            val serverHost = protocolType.serverHost(accountState.emailAddress, it.server.value)
            val nativeApiConfiguration = if (
                protocolType == IncomingProtocolType.GRAPH || protocolType == IncomingProtocolType.GMAIL_API
            ) {
                oauthConfigurationProvider?.getConfiguration(serverHost)
                    ?: NativeApiOAuthConfiguration.defaultFor(protocolType.defaultName, applicationId)
            } else {
                null
            }
            it.copy(
                protocolType = protocolType,
                preserveLegacyGraphProtocol =
                protocolType == IncomingProtocolType.GRAPH && it.preserveLegacyGraphProtocol,
                server = it.server.updateValue(serverHost),
                security = protocolType.defaultConnectionSecurity,
                port = it.port.updateValue(protocolType.toDefaultPort(protocolType.defaultConnectionSecurity)),
                authenticationType = protocolType.authenticationType(it.authenticationType),
                username = protocolType.username(accountState.emailAddress, it.username),
                password = if (protocolType.isNativeRestApi()) it.password.updateValue("") else it.password,
                clientCertificateAlias = it.clientCertificateAlias.takeUnless { protocolType.isNativeRestApi() },
                apiOAuthClientId = nativeApiConfiguration?.let { configuration ->
                    StringInputField(value = configuration.clientId)
                } ?: it.apiOAuthClientId,
                apiOAuthRedirectUri = nativeApiConfiguration?.let { configuration ->
                    StringInputField(value = configuration.redirectUri)
                } ?: it.apiOAuthRedirectUri,
                apiConfigurationError = false,
                zohoConfigurationError = false,
            )
        }
    }

    private fun updateSecurity(security: ConnectionSecurity) {
        updateState {
            it.copy(
                security = security,
                port = it.port.updateValue(it.protocolType.toDefaultPort(security)),
            )
        }
    }

    private fun submitConfig(forceOAuth: Boolean = false) {
        val validation = state.value.validate(validator, mode, applicationId)
        updateState {
            it.copy(
                server = it.server.updateFromValidationOutcome(validation.server),
                port = it.port.updateFromValidationOutcome(validation.port),
                username = it.username.updateFromValidationOutcome(validation.username),
                password = it.password.updateFromValidationOutcome(validation.password),
                imapPrefix = it.imapPrefix.updateFromValidationOutcome(validation.imapPrefix),
                apiConfigurationError = validation.apiConfigurationError,
                zohoConfigurationError = validation.zohoConfigurationError,
            )
        }

        if (!validation.hasError) {
            completeConfiguration(forceOAuth)
        }
    }

    private fun completeConfiguration(forceOAuth: Boolean) {
        val currentState = state.value
        if (currentState.protocolType == IncomingProtocolType.ZOHO_MAIL_API) {
            saveZohoAccountDetails(currentState)
        }
        accountStateRepository.setIncomingServerSettings(state.value.toServerSettings())
        if (forceOAuth && currentState.protocolType == IncomingProtocolType.ZOHO_MAIL_API) {
            accountStateRepository.setAuthorizationState(AuthorizationState())
        }
        navigateNext()
    }

    private fun saveZohoAccountDetails(currentState: State) {
        val emailAddress = currentState.username.value.trim()
        accountStateRepository.setEmailAddress(emailAddress)
        val existingOptions = accountStateRepository.getState().displayOptions
        accountStateRepository.setDisplayOptions(
            AccountDisplayOptions(
                accountName = existingOptions?.accountName?.takeIf(String::isNotBlank)
                    ?: emailAddress,
                displayName = existingOptions?.displayName.orEmpty(),
                emailSignature = existingOptions?.emailSignature,
            ),
        )
    }

    private fun onBack() {
        navigateBack()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}

private data class IncomingValidationResults(
    val server: ValidationOutcome,
    val port: ValidationOutcome,
    val username: ValidationOutcome,
    val password: ValidationOutcome,
    val imapPrefix: ValidationOutcome,
    val apiConfigurationError: Boolean,
    val zohoConfigurationError: Boolean,
) {
    val hasError: Boolean
        get() = listOf(server, port, username, password, imapPrefix).any { it is Outcome.Failure } ||
            apiConfigurationError || zohoConfigurationError
}

private fun State.validate(
    validator: Validator,
    mode: InteractionMode,
    applicationId: String,
): IncomingValidationResults {
    val isNativeApiAccount = protocolType.isNativeRestApi()
    return IncomingValidationResults(
        server = validateServer(validator, isNativeApiAccount),
        port = validatePort(validator, isNativeApiAccount),
        username = validateUsername(validator, isNativeApiAccount),
        password = validatePassword(validator, isNativeApiAccount),
        imapPrefix = validateImapPrefix(validator),
        apiConfigurationError = hasApiConfigurationError(mode, applicationId),
        zohoConfigurationError = hasZohoConfigurationError(applicationId),
    )
}

private fun State.validateServer(validator: Validator, isNativeApiAccount: Boolean): ValidationOutcome =
    if (isNativeApiAccount) ValidationSuccess else validator.validateServer(server.value)

private fun State.validatePort(validator: Validator, isNativeApiAccount: Boolean): ValidationOutcome =
    if (isNativeApiAccount) ValidationSuccess else validator.validatePort(port.value)

private fun State.validateUsername(validator: Validator, isNativeApiAccount: Boolean): ValidationOutcome =
    if (isNativeApiAccount) ValidationSuccess else validator.validateUsername(username.value)

private fun State.validatePassword(validator: Validator, isNativeApiAccount: Boolean): ValidationOutcome =
    if (!isNativeApiAccount && authenticationType.isPasswordRequired) {
        validator.validatePassword(password.value)
    } else {
        ValidationSuccess
    }

private fun State.validateImapPrefix(validator: Validator): ValidationOutcome =
    if (protocolType == IncomingProtocolType.IMAP) validator.validateImapPrefix(imapPrefix.value) else ValidationSuccess

private fun State.hasApiConfigurationError(mode: InteractionMode, applicationId: String): Boolean {
    val isNativeApiOAuthAccount = protocolType == IncomingProtocolType.GRAPH ||
        protocolType == IncomingProtocolType.GMAIL_API
    val hasMissingValue = apiOAuthClientId.value.isBlank() || apiOAuthRedirectUri.value.isBlank()
    val hasInvalidRedirectUri = applicationId.isNotBlank() &&
        !isRegisteredOAuthRedirectUri(apiOAuthRedirectUri.value, applicationId)

    return mode == InteractionMode.Create && isNativeApiOAuthAccount && (hasMissingValue || hasInvalidRedirectUri)
}

private fun State.hasZohoConfigurationError(applicationId: String): Boolean {
    val hasMissingValue = username.value.isBlank() || zohoClientId.value.isBlank() || zohoRedirectUri.value.isBlank()
    val hasInvalidRedirectUri = applicationId.isNotBlank() &&
        !isRegisteredOAuthRedirectUri(zohoRedirectUri.value, applicationId)

    return protocolType == IncomingProtocolType.ZOHO_MAIL_API && (hasMissingValue || hasInvalidRedirectUri)
}

private fun IncomingProtocolType.authenticationType(current: AuthenticationType): AuthenticationType =
    if (isNativeRestApi()) {
        AuthenticationType.OAuth2
    } else {
        current.takeIf { it in allowedAuthenticationTypes } ?: allowedAuthenticationTypes.first()
    }

private fun IncomingProtocolType.serverHost(emailAddress: String?, currentHost: String): String = when {
    this == IncomingProtocolType.GRAPH -> GRAPH_HOST
    this == IncomingProtocolType.GMAIL_API -> GMAIL_API_HOST
    isNativeRestApi() -> emailAddress?.toInvalidEmailDomain().orEmpty()
    else -> currentHost
}

private fun IncomingProtocolType.username(emailAddress: String?, current: StringInputField): StringInputField = when {
    this == IncomingProtocolType.ZOHO_MAIL_API -> current.updateValue(emailAddress.orEmpty())
    isNativeRestApi() && current.value.isBlank() -> current.updateValue(emailAddress.orEmpty())
    else -> current
}

private const val GRAPH_HOST = "graph.microsoft.com"
private const val GMAIL_API_HOST = "gmail.googleapis.com"
