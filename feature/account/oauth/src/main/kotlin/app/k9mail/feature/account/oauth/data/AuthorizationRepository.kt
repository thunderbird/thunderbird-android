package app.k9mail.feature.account.oauth.data

import android.content.Intent
import androidx.core.net.toUri
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.DomainContract
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class AuthorizationRepository(
    private val service: AuthorizationService,
) : DomainContract.AuthorizationRepository {

    override fun getAuthorizationRequestIntent(
        configuration: OAuthConfiguration,
        emailAddress: String,
    ): AuthorizationIntentResult {
        return AuthorizationIntentResult.Success(
            createAuthorizationRequestIntent(configuration, emailAddress),
        )
    }

    private fun createAuthorizationRequestIntent(configuration: OAuthConfiguration, emailAddress: String): Intent {
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
