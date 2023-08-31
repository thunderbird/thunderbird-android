package app.k9mail.feature.account.oauth.domain

import android.content.Intent
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class FakeAuthorizationRepository(
    private val answerGetAuthorizationRequestIntent: AuthorizationIntentResult = AuthorizationIntentResult.NotSupported,
    private val answerGetAuthorizationResponse: AuthorizationResponse? = null,
    private val answerGetAuthorizationException: AuthorizationException? = null,
    private val answerGetExchangeToken: AuthorizationResult = AuthorizationResult.Canceled,
) : AccountOAuthDomainContract.AuthorizationRepository {

    var recordedGetAuthorizationRequestIntentConfiguration: OAuthConfiguration? = null
    var recordedGetAuthorizationRequestIntentEmailAddress: String? = null
    override fun getAuthorizationRequestIntent(
        configuration: OAuthConfiguration,
        emailAddress: String,
    ): AuthorizationIntentResult {
        recordedGetAuthorizationRequestIntentConfiguration = configuration
        recordedGetAuthorizationRequestIntentEmailAddress = emailAddress
        return answerGetAuthorizationRequestIntent
    }

    var recordedGetAuthorizationResponseIntent: Intent? = null

    override suspend fun getAuthorizationResponse(intent: Intent): AuthorizationResponse? {
        recordedGetAuthorizationResponseIntent = intent
        return answerGetAuthorizationResponse
    }

    var recordedGetAuthorizationExceptionIntent: Intent? = null

    override suspend fun getAuthorizationException(intent: Intent): AuthorizationException? {
        recordedGetAuthorizationExceptionIntent = intent
        return answerGetAuthorizationException
    }

    var recordedGetExchangeTokenResponse: AuthorizationResponse? = null

    override suspend fun getExchangeToken(
        response: AuthorizationResponse,
    ): AuthorizationResult {
        recordedGetExchangeTokenResponse = response
        return answerGetExchangeToken
    }
}
