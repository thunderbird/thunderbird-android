package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountAuthorizationStateUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.domain.ApiAccountReauthorizationResult
import app.k9mail.feature.account.edit.domain.GmailAccountIdentityVerifier
import app.k9mail.feature.account.oauth.domain.NativeApiOAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.domain.isRegisteredOAuthRedirectUri
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.feature.account.AccountId

class ReauthorizeNativeApiAccount(
    private val loadAccountState: AccountEditDomainContract.UseCase.LoadAccountState,
    private val accountAuthorizationStateUpdater: AccountAuthorizationStateUpdater,
    private val oauthConfigurationProvider: OAuthConfigurationProvider,
    private val gmailIdentityVerifier: GmailAccountIdentityVerifier,
    private val applicationId: String = "",
) : AccountEditDomainContract.UseCase.ReauthorizeNativeApiAccount {

    override suspend fun execute(accountId: AccountId, result: OAuthResult): ApiAccountReauthorizationResult =
        when (result) {
            is OAuthResult.Success -> reauthorizeSuccessfully(accountId, result.authorizationState)
            OAuthResult.Failure -> ApiAccountReauthorizationResult.Canceled
        }

    private suspend fun reauthorizeSuccessfully(
        accountId: AccountId,
        newAuthorizationState: AuthorizationState,
    ): ApiAccountReauthorizationResult {
        val accountState = runCatching { loadAccountState.execute(accountId) }.getOrNull()
        val serverSettings = accountState?.incomingServerSettings
        return when {
            accountState == null -> ApiAccountReauthorizationResult.Failed

            serverSettings == null -> ApiAccountReauthorizationResult.NotAvailable

            else -> reauthorizeAccountState(
                accountId,
                accountState,
                serverSettings,
                newAuthorizationState,
            )
        }
    }

    private suspend fun reauthorizeAccountState(
        accountId: AccountId,
        accountState: AccountState,
        serverSettings: ServerSettings,
        newAuthorizationState: AuthorizationState,
    ): ApiAccountReauthorizationResult {
        val configuration = when (serverSettings.type) {
            Protocols.GMAIL_API -> (
                oauthConfigurationProvider.getConfiguration(serverSettings.host)
                    ?: NativeApiOAuthConfiguration.defaultFor(serverSettings.type, applicationId)
                )
                ?.let { configuration ->
                    val clientId = serverSettings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID]
                        ?.takeIf(String::isNotBlank)
                        ?: configuration.clientId
                    val redirectUri = serverSettings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI]
                        ?.takeIf(String::isNotBlank)
                        ?: configuration.redirectUri
                    configuration.copy(clientId = clientId, redirectUri = redirectUri)
                }

            Protocols.ZOHO_MAIL_API -> serverSettings.toZohoOAuthConfiguration()

            else -> null
        }?.takeIf {
            it.isComplete() && (applicationId.isBlank() || isRegisteredOAuthRedirectUri(it.redirectUri, applicationId))
        }

        return when {
            configuration == null -> ApiAccountReauthorizationResult.NotAvailable

            serverSettings.type == Protocols.GMAIL_API &&
                !gmailIdentityVerifier.matches(accountState.emailAddress, newAuthorizationState) ->
                ApiAccountReauthorizationResult.GmailAccountMismatch

            else -> persistAuthorizationState(accountId, accountState, newAuthorizationState)
        }
    }

    private suspend fun persistAuthorizationState(
        accountId: AccountId,
        accountState: AccountState,
        newAuthorizationState: AuthorizationState,
    ): ApiAccountReauthorizationResult =
        when (
            accountAuthorizationStateUpdater.updateAuthorizationState(
                accountId = accountId,
                expectedAuthorizationState = accountState.authorizationState,
                newAuthorizationState = newAuthorizationState,
            )
        ) {
            is AccountUpdaterResult.Success -> ApiAccountReauthorizationResult.Success
            is AccountUpdaterResult.Failure -> ApiAccountReauthorizationResult.Failed
        }

    private fun ServerSettings.toZohoOAuthConfiguration(): OAuthConfiguration? {
        val zohoClientId = extra[ZohoMailServerSettings.EXTRA_CLIENT_ID].orEmpty()
        val redirectUri = extra[ZohoMailServerSettings.EXTRA_REDIRECT_URI].orEmpty()
        if (zohoClientId.isBlank() || redirectUri.isBlank()) return null

        return OAuthConfiguration(
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

    private fun OAuthConfiguration.isComplete(): Boolean =
        clientId.isNotBlank() &&
            scopes.isNotEmpty() && scopes.all(String::isNotBlank) &&
            authorizationEndpoint.isNotBlank() &&
            tokenEndpoint.isNotBlank() &&
            redirectUri.isNotBlank()
}
