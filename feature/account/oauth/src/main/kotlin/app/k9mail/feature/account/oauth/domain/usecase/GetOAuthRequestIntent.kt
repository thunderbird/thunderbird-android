package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.autodiscovery.api.OAuthSettings
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult

internal class GetOAuthRequestIntent(
    private val repository: AccountOAuthDomainContract.AuthorizationRepository,
    private val configurationProvider: OAuthConfigurationProvider,
) : GetOAuthRequestIntent {
    override fun execute(
        hostname: String,
        emailAddress: String,
        oAuthSettings: OAuthSettings?,
    ): AuthorizationIntentResult {
        val configuration = if (oAuthSettings != null) {
            OAuthConfiguration(
                clientId = "myapp",
                scopes = oAuthSettings.scopes,
                authorizationEndpoint = oAuthSettings.authorizationEndpoint,
                tokenEndpoint = oAuthSettings.tokenEndpoint,
                redirectUri = "com.fsck.k9.debug://oauth2redirect",
            )
        } else {
            configurationProvider.getConfiguration(hostname)
                ?: return AuthorizationIntentResult.NotSupported
        }

        return repository.getAuthorizationRequestIntent(configuration, emailAddress)
    }
}
