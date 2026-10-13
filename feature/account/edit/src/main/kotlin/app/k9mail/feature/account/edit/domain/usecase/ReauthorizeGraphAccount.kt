package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountAuthorizationStateUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.GraphAccountReauthorizationResult
import app.k9mail.feature.account.oauth.domain.NativeApiOAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.feature.account.AccountId

class ReauthorizeGraphAccount(
    private val loadAccountState: AccountEditDomainContract.UseCase.LoadAccountState,
    private val accountAuthorizationStateUpdater: AccountAuthorizationStateUpdater,
    private val oauthConfigurationProvider: OAuthConfigurationProvider,
    private val identityVerifier: AccountEditDomainContract.MicrosoftAccountIdentityVerifier,
    private val applicationId: String = "",
) : AccountEditDomainContract.UseCase.ReauthorizeGraphAccount {

    override suspend fun execute(accountId: AccountId, result: OAuthResult): GraphAccountReauthorizationResult =
        when (result) {
            is OAuthResult.Success -> reauthorizeSuccessfully(accountId, result.authorizationState)
            OAuthResult.Failure -> GraphAccountReauthorizationResult.Canceled
        }

    private suspend fun reauthorizeSuccessfully(
        accountId: AccountId,
        newAuthorizationState: AuthorizationState,
    ): GraphAccountReauthorizationResult {
        val currentAccountState = runCatching { loadAccountState.execute(accountId) }.getOrNull()
        val serverSettings = currentAccountState?.incomingServerSettings
        val configuration = serverSettings
            ?.takeIf { Protocols.isGraphProtocol(it.type) }
            ?.let { serverSettings ->
                (
                    oauthConfigurationProvider.getConfiguration(serverSettings.host)
                        ?: NativeApiOAuthConfiguration.defaultFor(serverSettings.type, applicationId)
                    )?.let { configuration ->
                    val clientId = serverSettings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID]
                        ?.takeIf(String::isNotBlank)
                        ?: configuration.clientId
                    val redirectUri = serverSettings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI]
                        ?.takeIf(String::isNotBlank)
                        ?: configuration.redirectUri
                    configuration.copy(clientId = clientId, redirectUri = redirectUri)
                }
            }
            ?.takeIf {
                it.isComplete() &&
                    (applicationId.isBlank() || isRegisteredOAuthRedirectUri(it.redirectUri, applicationId))
            }

        return when {
            currentAccountState == null -> GraphAccountReauthorizationResult.Failed

            serverSettings?.type?.let { !Protocols.isGraphProtocol(it) } != false -> {
                GraphAccountReauthorizationResult.NotAvailable
            }

            configuration == null -> GraphAccountReauthorizationResult.NotAvailable

            !identityVerifier.matches(
                accountEmail = currentAccountState.emailAddress,
                currentAuthorizationState = currentAccountState.authorizationState,
                newAuthorizationState = newAuthorizationState,
                clientId = configuration.clientId,
            ) -> GraphAccountReauthorizationResult.MicrosoftAccountMismatch

            else -> persistAuthorizationState(
                accountId = accountId,
                currentAccountState = currentAccountState,
                newAuthorizationState = newAuthorizationState,
            )
        }
    }

    private suspend fun persistAuthorizationState(
        accountId: AccountId,
        currentAccountState: AccountState,
        newAuthorizationState: AuthorizationState,
    ): GraphAccountReauthorizationResult =
        when (
            accountAuthorizationStateUpdater.updateAuthorizationState(
                accountId = accountId,
                expectedAuthorizationState = currentAccountState.authorizationState,
                newAuthorizationState = newAuthorizationState,
            )
        ) {
            is AccountUpdaterResult.Success -> GraphAccountReauthorizationResult.Success
            is AccountUpdaterResult.Failure -> GraphAccountReauthorizationResult.Failed
        }

    private fun OAuthConfiguration.isComplete(): Boolean =
        clientId.isNotBlank() &&
            scopes.isNotEmpty() && scopes.all(String::isNotBlank) &&
            authorizationEndpoint.isNotBlank() &&
            tokenEndpoint.isNotBlank() &&
            redirectUri.isNotBlank()
}
