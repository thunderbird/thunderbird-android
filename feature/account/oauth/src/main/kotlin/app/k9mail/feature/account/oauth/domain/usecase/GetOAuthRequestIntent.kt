package app.k9mail.feature.account.oauth.domain.usecase

import android.content.Intent
import androidx.core.net.toUri
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase.GetOAuthRequestIntent.GetOAuthRequestIntentResult
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

internal class GetOAuthRequestIntent(
    private val service: AuthorizationService,
    private val configurationProvider: OAuthConfigurationProvider,
) : GetOAuthRequestIntent {
    override suspend fun execute(hostname: String, emailAddress: String): GetOAuthRequestIntentResult {
        val configuration = configurationProvider.getConfiguration(hostname)
            ?: return GetOAuthRequestIntentResult.NotSupported

        return GetOAuthRequestIntentResult.Success(createAuthorizationRequestIntent(emailAddress, configuration))
    }

    private fun createAuthorizationRequestIntent(emailAddress: String, configuration: OAuthConfiguration): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            configuration.authorizationEndpoint.toUri(),
            configuration.tokenEndpoint.toUri(),
        )

        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            configuration.clientId,
            ResponseTypeValues.CODE,
            configuration.redirectUri.toUri(),
        )

        val authRequest = authRequestBuilder
            .setScope(configuration.scopes.joinToString(" "))
            .setCodeVerifier(null)
            .setLoginHint(emailAddress)
            .build()

        return service.getAuthorizationRequestIntent(authRequest)
    }
}
