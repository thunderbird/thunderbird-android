package app.k9mail.feature.account.server.validation.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.ui.WizardConstants
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.NativeApiOAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.domain.entity.isOAuth
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.validation.domain.OAuthAccountIdentityResolver
import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Effect
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

@Suppress("TooManyFunctions")
abstract class BaseServerValidationViewModel(
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    private val validateServerSettings: ServerValidationDomainContract.UseCase.ValidateServerSettings,
    private val authorizationStateRepository: AccountOAuthDomainContract.AuthorizationStateRepository,
    private val certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
    override val oAuthViewModel: AccountOAuthContract.ViewModel,
    override val isIncomingValidation: Boolean = true,
    initialState: State? = null,
    private val accountIdentityResolver: OAuthAccountIdentityResolver? = null,
    private val oauthConfigurationProvider: OAuthConfigurationProvider? = null,
    private val applicationId: String = "",
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState ?: accountStateRepository.getState().toServerValidationState(isIncomingValidation),
),
    ServerValidationContract.ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountStateAndValidate -> handleOneTimeEvent(event, ::loadAccountStateAndValidate)
            is Event.OnOAuthResult -> onOAuthResult(event.result)
            Event.ValidateServerSettings -> onValidateConfig()
            Event.OnNextClicked -> navigateNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> onRetry()
            Event.OnCertificateAccepted -> onRetry()
        }
    }

    private fun loadAccountStateAndValidate() {
        updateState {
            accountStateRepository.getState().toServerValidationState(isIncomingValidation)
        }
        onValidateConfig()
    }

    private fun onValidateConfig() {
        if (state.value.isSuccess) {
            navigateNext()
        } else if (state.value.serverSettings.isOAuth()) {
            checkOAuthState()
        } else {
            validateServerSettings()
        }
    }

    private fun checkOAuthState() {
        val authorizationState = accountStateRepository.getState().authorizationState
        if (authorizationState != null) {
            if (authorizationStateRepository.isAuthorized(authorizationState)) {
                validateServerSettings()
            } else {
                startOAuthSignIn()
            }
        } else {
            startOAuthSignIn()
        }
    }

    private fun startOAuthSignIn() {
        val currentState = state.value
        val incomingSettings = accountStateRepository.getState().incomingServerSettings
        val oauthConfiguration = incomingSettings.toOAuthConfiguration()

        if (incomingSettings.isNativeApiOAuthSettings() && oauthConfiguration == null) {
            updateError(Error.UnknownError("OAuth Client ID or app callback URI is not configured"))
            return
        }

        val hostname = currentState.serverSettings?.host
        val emailAddress = currentState.emailAddress
        if (hostname == null || emailAddress == null) {
            updateError(Error.UnknownError("Hostname or email address not set"))
            return
        }

        updateState { it.copy(needsAuthorization = true) }
        oAuthViewModel.initState(
            AccountOAuthContract.State(
                hostname = hostname,
                emailAddress = emailAddress,
                oauthConfiguration = oauthConfiguration,
            ),
        )
    }

    private fun ServerSettings?.toOAuthConfiguration(): OAuthConfiguration? {
        if (this == null) return null

        return when {
            type == Protocols.ZOHO_MAIL_API -> zohoOAuthConfiguration(this)
            type == Protocols.GMAIL_API || Protocols.isGraphProtocol(type) -> nativeApiOAuthConfiguration(this)
            else -> null
        }
    }

    private fun zohoOAuthConfiguration(settings: ServerSettings): OAuthConfiguration? {
        val clientId = settings.extra[ZohoMailServerSettings.EXTRA_CLIENT_ID].orEmpty()
        val redirectUri = settings.extra[ZohoMailServerSettings.EXTRA_REDIRECT_URI].orEmpty()

        return when {
            clientId.isBlank() -> null

            redirectUri.isBlank() -> null

            !isValidRedirectUri(redirectUri) -> null

            else -> OAuthConfiguration(
                clientId = clientId,
                scopes = ZohoMailServerSettings.OAUTH_SCOPES,
                authorizationEndpoint = ZohoMailServerSettings.AUTHORIZATION_ENDPOINT,
                tokenEndpoint = ZohoMailServerSettings.TOKEN_ENDPOINT,
                redirectUri = redirectUri,
                clientSecret = settings.password,
                scopeSeparator = ZohoMailServerSettings.SCOPE_SEPARATOR,
                authorizationParameters = mapOf("access_type" to "offline", "prompt" to "consent"),
            )
        }
    }

    private fun nativeApiOAuthConfiguration(settings: ServerSettings): OAuthConfiguration? {
        val baseConfiguration = oauthConfigurationProvider?.getConfiguration(settings.host)
            ?: NativeApiOAuthConfiguration.defaultFor(settings.type, applicationId)

        return baseConfiguration?.let { configuration ->
            configuration.copy(
                clientId = settings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID]
                    ?.takeIf(String::isNotBlank) ?: configuration.clientId,
                redirectUri = settings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI]
                    ?.takeIf(String::isNotBlank) ?: configuration.redirectUri,
            )
        }?.takeIf { it.clientId.isNotBlank() && isValidRedirectUri(it.redirectUri) }
    }

    private fun isValidRedirectUri(redirectUri: String): Boolean =
        applicationId.isBlank() || isRegisteredOAuthRedirectUri(redirectUri, applicationId)

    private fun ServerSettings?.isNativeApiOAuthSettings(): Boolean =
        this != null && Protocols.isNativeApiProtocol(type)

    private fun onOAuthResult(result: OAuthResult) {
        if (result is OAuthResult.Success) {
            accountStateRepository.setAuthorizationState(result.authorizationState)
            updateState {
                it.copy(
                    needsAuthorization = false,
                )
            }

            if (isIncomingValidation && accountIdentityResolver != null) {
                viewModelScope.launch {
                    applyProviderIdentity(result.authorizationState)
                    validateServerSettings()
                }
            } else {
                validateServerSettings()
            }
        }
    }

    private suspend fun applyProviderIdentity(authorizationState: AuthorizationState) {
        val serverSettings = state.value.serverSettings ?: return
        val identity = accountIdentityResolver?.resolve(serverSettings, authorizationState) ?: return
        val profileEmail = identity.emailAddress?.takeIf(String::isValidEmailAddress)
        val profileName = identity.displayName?.trim()?.takeIf(String::isNotEmpty)
        val accountState = accountStateRepository.getState()
        val resolvedEmail = profileEmail ?: accountState.emailAddress
        val updatedServerSettings = if (profileEmail != null) {
            serverSettings.copy(username = profileEmail)
        } else {
            serverSettings
        }

        if (profileEmail != null) {
            accountStateRepository.setEmailAddress(profileEmail)
            accountStateRepository.setIncomingServerSettings(updatedServerSettings)
        }

        if (profileName != null) {
            val existingOptions = accountState.displayOptions
            accountStateRepository.setDisplayOptions(
                AccountDisplayOptions(
                    accountName = existingOptions?.accountName?.takeIf(String::isNotBlank)
                        ?: resolvedEmail.orEmpty(),
                    displayName = profileName,
                    emailSignature = existingOptions?.emailSignature,
                ),
            )
        }

        updateState {
            it.copy(
                emailAddress = resolvedEmail,
                serverSettings = updatedServerSettings,
            )
        }
    }

    private fun validateServerSettings() {
        viewModelScope.launch {
            val serverSettings = state.value.serverSettings
            if (serverSettings == null) {
                updateError(Error.UnknownError("Server settings not set"))
                return@launch
            }

            updateState {
                it.copy(isLoading = true)
            }

            when (val result = validateServerSettings.execute(serverSettings)) {
                ServerSettingsValidationResult.Success -> updateSuccess()

                is ServerSettingsValidationResult.AuthenticationError -> updateError(
                    Error.AuthenticationError(result.serverMessage),
                )

                is ServerSettingsValidationResult.CertificateError -> updateError(
                    Error.CertificateError(result.certificateChain),
                )

                ServerSettingsValidationResult.ClientCertificateError.ClientCertificateExpired -> updateError(
                    Error.ClientCertificateExpired,
                )

                ServerSettingsValidationResult.ClientCertificateError.ClientCertificateRetrievalFailure -> updateError(
                    Error.ClientCertificateRetrievalFailure,
                )

                is ServerSettingsValidationResult.NetworkError -> updateError(
                    Error.NetworkError(result.exception),
                )

                is ServerSettingsValidationResult.ServerError -> updateError(
                    Error.ServerError(result.serverMessage),
                )

                is ServerSettingsValidationResult.MissingServerCapabilityError -> updateError(
                    Error.MissingServerCapabilityError(result.capabilityName),
                )

                is ServerSettingsValidationResult.UnknownError -> updateError(
                    Error.UnknownError(result.exception.message ?: "Unknown error"),
                )
            }
        }
    }

    private fun updateSuccess() {
        updateState {
            it.copy(
                isLoading = false,
                isSuccess = true,
            )
        }

        viewModelScope.launch {
            delay(WizardConstants.CONTINUE_NEXT_DELAY)
            navigateNext()
        }
    }

    private fun updateError(error: Error) {
        if (error is Error.CertificateError) {
            val serverSettings = checkNotNull(state.value.serverSettings)

            certificateErrorRepository.setCertificateError(
                ServerCertificateError(
                    hostname = serverSettings.host,
                    port = serverSettings.port,
                    certificateChain = error.certificateChain,
                ),
            )
        }

        updateState {
            it.copy(
                error = error,
                isLoading = false,
                isSuccess = false,
            )
        }
    }

    private fun onBack() {
        navigateBack()
    }

    private fun onRetry() {
        updateState {
            it.copy(
                error = null,
            )
        }
        onValidateConfig()
    }

    private fun navigateBack() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateBack)
    }

    private fun navigateNext() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateNext)
    }
}

private fun String.isValidEmailAddress(): Boolean {
    val separator = indexOf('@')
    return separator > 0 && separator == lastIndexOf('@') &&
        separator < lastIndex && !any(Char::isWhitespace)
}
