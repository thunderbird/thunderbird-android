package app.k9mail.feature.account.edit.ui.server.settings.reauthorize

import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.ApiAccountReauthorizationResult
import app.k9mail.feature.account.edit.domain.GraphAccountReauthorizationResult
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationContract.Error
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationContract.Provider
import app.k9mail.feature.account.oauth.domain.NativeApiOAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.launch
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.AccountId
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State as OAuthState

class ApiAccountReauthorizationViewModel(
    private val accountId: AccountId,
    private val loadAccountState: AccountEditDomainContract.UseCase.LoadAccountState,
    private val oauthConfigurationProvider: OAuthConfigurationProvider,
    private val reauthorizeGraphAccount: AccountEditDomainContract.UseCase.ReauthorizeGraphAccount,
    private val reauthorizeNativeApiAccount: AccountEditDomainContract.UseCase.ReauthorizeNativeApiAccount,
    override val accountOAuthViewModel: AccountOAuthContract.ViewModel,
    private val applicationId: String = "",
    initialState: ApiAccountReauthorizationContract.State = ApiAccountReauthorizationContract.State(),
) : BaseViewModel<
    ApiAccountReauthorizationContract.State,
    ApiAccountReauthorizationContract.Event,
    ApiAccountReauthorizationContract.Effect,
    >(initialState),
    ApiAccountReauthorizationContract.ViewModel {

    override fun event(event: ApiAccountReauthorizationContract.Event) {
        when (event) {
            ApiAccountReauthorizationContract.Event.Load -> handleOneTimeEvent(event, ::loadAccountState)
            is ApiAccountReauthorizationContract.Event.OnOAuthResult -> onOAuthResult(event.result)
        }
    }

    private fun loadAccountState() {
        viewModelScope.launch {
            val accountState = runCatching { loadAccountState.execute(accountId) }.getOrNull()
            val serverSettings = accountState?.incomingServerSettings
            val provider = serverSettings?.type?.toProvider()
            val configuration = serverSettings?.let { settings ->
                when (settings.type) {
                    Protocols.GRAPH, Protocols.GRAPH_LEGACY, Protocols.GMAIL_API ->
                        (
                            oauthConfigurationProvider.getConfiguration(settings.host)
                                ?: NativeApiOAuthConfiguration.defaultFor(settings.type, applicationId)
                            )?.let { configuration ->
                            val clientId = settings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID]
                                ?.takeIf(String::isNotBlank)
                                ?: configuration.clientId
                            val redirectUri = settings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI]
                                ?.takeIf(String::isNotBlank)
                                ?: configuration.redirectUri
                            configuration.copy(clientId = clientId, redirectUri = redirectUri)
                        }

                    Protocols.ZOHO_MAIL_API -> settings.toZohoOAuthConfiguration()

                    else -> null
                }
            }
            val emailAddress = accountState?.emailAddress?.takeIf(String::isNotBlank)

            if (serverSettings == null || provider == null) {
                markNotAvailable()
                return@launch
            }
            if (configuration?.isCompleteForReauthorization() != true) {
                markNotAvailable()
                return@launch
            }
            if (applicationId.isNotBlank() && !isRegisteredOAuthRedirectUri(configuration.redirectUri, applicationId)) {
                markNotAvailable()
                return@launch
            }
            if (emailAddress == null) {
                markNotAvailable()
                return@launch
            }

            accountOAuthViewModel.initState(
                OAuthState(
                    hostname = serverSettings.host,
                    emailAddress = emailAddress,
                    oauthConfiguration = configuration,
                ),
            )
            updateState { it.copy(provider = provider, isInitializing = false) }
        }
    }

    private fun onOAuthResult(result: OAuthResult) {
        if (result !is OAuthResult.Success) {
            updateState { it.copy(error = Error.Canceled) }
            return
        }

        val provider = state.value.provider
        viewModelScope.launch {
            updateState { it.copy(isSaving = true, error = null) }
            val outcome = when (provider) {
                Provider.Microsoft -> reauthorizeGraphAccount.execute(accountId, result).toViewModelOutcome()

                Provider.Google, Provider.Zoho -> reauthorizeNativeApiAccount.execute(
                    accountId,
                    result,
                ).toViewModelOutcome()

                null -> Error.NotAvailable
            }
            updateState {
                it.copy(
                    isSaving = false,
                    isSuccess = outcome == null,
                    error = outcome,
                )
            }
        }
    }

    private fun String.toProvider(): Provider? = when (this) {
        Protocols.GRAPH, Protocols.GRAPH_LEGACY -> Provider.Microsoft
        Protocols.GMAIL_API -> Provider.Google
        Protocols.ZOHO_MAIL_API -> Provider.Zoho
        else -> null
    }

    private fun GraphAccountReauthorizationResult.toViewModelOutcome(): Error? = when (this) {
        GraphAccountReauthorizationResult.Success -> null
        GraphAccountReauthorizationResult.Canceled -> Error.Canceled
        GraphAccountReauthorizationResult.NotAvailable -> Error.NotAvailable
        GraphAccountReauthorizationResult.MicrosoftAccountMismatch -> Error.IdentityMismatch
        GraphAccountReauthorizationResult.Failed -> Error.Failed
    }

    private fun ApiAccountReauthorizationResult.toViewModelOutcome(): Error? = when (this) {
        ApiAccountReauthorizationResult.Success -> null
        ApiAccountReauthorizationResult.Canceled -> Error.Canceled
        ApiAccountReauthorizationResult.NotAvailable -> Error.NotAvailable
        ApiAccountReauthorizationResult.GmailAccountMismatch -> Error.IdentityMismatch
        ApiAccountReauthorizationResult.Failed -> Error.Failed
    }

    private fun OAuthConfiguration.isCompleteForReauthorization(): Boolean =
        clientId.isNotBlank() &&
            scopes.isNotEmpty() && scopes.all(String::isNotBlank) &&
            authorizationEndpoint.isNotBlank() &&
            tokenEndpoint.isNotBlank() &&
            redirectUri.isNotBlank()

    private fun markNotAvailable() {
        updateState { it.copy(isInitializing = false, error = Error.NotAvailable) }
    }

    private fun ServerSettings.toZohoOAuthConfiguration(): OAuthConfiguration? {
        val zohoClientId = extra[ZohoMailServerSettings.EXTRA_CLIENT_ID].orEmpty()
        val redirectUri = extra[ZohoMailServerSettings.EXTRA_REDIRECT_URI].orEmpty()
        return when {
            zohoClientId.isBlank() || redirectUri.isBlank() -> null

            applicationId.isNotBlank() && !isRegisteredOAuthRedirectUri(redirectUri, applicationId) -> null

            else -> OAuthConfiguration(
                clientId = zohoClientId,
                scopes = ZohoMailServerSettings.OAUTH_SCOPES,
                authorizationEndpoint = ZohoMailServerSettings.AUTHORIZATION_ENDPOINT,
                tokenEndpoint = ZohoMailServerSettings.TOKEN_ENDPOINT,
                redirectUri = redirectUri,
                clientSecret = password,
                scopeSeparator = ZohoMailServerSettings.SCOPE_SEPARATOR,
                authorizationParameters = mapOf("access_type" to "offline", "prompt" to "consent"),
            )
        }
    }
}
