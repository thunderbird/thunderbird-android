package app.k9mail.feature.account.edit.ui.server.settings.reauthorize

import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.GraphAccountReauthorizationResult
import app.k9mail.feature.account.oauth.domain.NativeApiOAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import kotlinx.coroutines.launch
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.AccountId
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State as OAuthState

class GraphAccountReauthorizationViewModel(
    private val accountId: AccountId,
    private val loadAccountState: AccountEditDomainContract.UseCase.LoadAccountState,
    private val reauthorizeGraphAccount: AccountEditDomainContract.UseCase.ReauthorizeGraphAccount,
    private val oauthConfigurationProvider: OAuthConfigurationProvider,
    override val accountOAuthViewModel: AccountOAuthContract.ViewModel,
    private val applicationId: String = "",
    initialState: GraphAccountReauthorizationContract.State = GraphAccountReauthorizationContract.State(),
) : BaseViewModel<
    GraphAccountReauthorizationContract.State,
    GraphAccountReauthorizationContract.Event,
    GraphAccountReauthorizationContract.Effect,
    >(initialState),
    GraphAccountReauthorizationContract.ViewModel {

    override fun event(event: GraphAccountReauthorizationContract.Event) {
        when (event) {
            GraphAccountReauthorizationContract.Event.Load -> handleOneTimeEvent(event, ::loadAccountState)
            is GraphAccountReauthorizationContract.Event.OnOAuthResult -> onOAuthResult(event.result)
        }
    }

    private fun loadAccountState() {
        viewModelScope.launch {
            val accountState = runCatching { loadAccountState.execute(accountId) }.getOrNull()
            val serverSettings = accountState?.incomingServerSettings
            val graphServerSettings = serverSettings?.takeIf { Protocols.isGraphProtocol(it.type) }
            val configuration = graphServerSettings?.let { settings ->
                val baseConfiguration = oauthConfigurationProvider.getConfiguration(settings.host)
                    ?: NativeApiOAuthConfiguration.defaultFor(settings.type, applicationId)
                baseConfiguration?.let {
                    it.copy(
                        clientId = settings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID]
                            ?.takeIf(String::isNotBlank) ?: it.clientId,
                        redirectUri = settings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI]
                            ?.takeIf(String::isNotBlank) ?: it.redirectUri,
                    )
                }
            }

            val emailAddress = accountState?.emailAddress?.takeIf(String::isNotBlank)
            if (graphServerSettings == null) {
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
                    hostname = graphServerSettings.host,
                    emailAddress = emailAddress,
                    oauthConfiguration = configuration,
                ),
            )
            updateState { it.copy(isInitializing = false) }
        }
    }

    private fun markNotAvailable() {
        updateState {
            it.copy(isInitializing = false, error = GraphAccountReauthorizationContract.Error.NotAvailable)
        }
    }

    private fun onOAuthResult(result: OAuthResult) {
        if (result !is OAuthResult.Success) {
            updateState {
                it.copy(error = GraphAccountReauthorizationContract.Error.Canceled)
            }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSaving = true, error = null) }
            val outcome = reauthorizeGraphAccount.execute(accountId, result)
            updateState {
                it.copy(
                    isSaving = false,
                    isSuccess = outcome == GraphAccountReauthorizationResult.Success,
                    error = outcome.toViewModelError(),
                )
            }
        }
    }

    private fun GraphAccountReauthorizationResult.toViewModelError(): GraphAccountReauthorizationContract.Error? =
        when (this) {
            GraphAccountReauthorizationResult.Success -> null

            GraphAccountReauthorizationResult.Canceled -> GraphAccountReauthorizationContract.Error.Canceled

            GraphAccountReauthorizationResult.NotAvailable -> GraphAccountReauthorizationContract.Error.NotAvailable

            GraphAccountReauthorizationResult.MicrosoftAccountMismatch -> {
                GraphAccountReauthorizationContract.Error.MicrosoftAccountMismatch
            }

            GraphAccountReauthorizationResult.Failed -> GraphAccountReauthorizationContract.Error.Failed
        }

    private fun net.thunderbird.core.common.oauth.OAuthConfiguration.isCompleteForReauthorization(): Boolean =
        clientId.isNotBlank() &&
            scopes.isNotEmpty() && scopes.all(String::isNotBlank) &&
            authorizationEndpoint.isNotBlank() &&
            tokenEndpoint.isNotBlank() &&
            redirectUri.isNotBlank()
}
