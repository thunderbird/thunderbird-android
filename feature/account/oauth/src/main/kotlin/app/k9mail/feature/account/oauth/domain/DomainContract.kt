package app.k9mail.feature.account.oauth.domain

import android.content.Intent
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

interface DomainContract {

    interface UseCase {
        fun interface SuggestServerName {
            fun suggest(protocol: String, domain: String): String
        }

        fun interface GetOAuthRequestIntent {
            fun execute(hostname: String, emailAddress: String): AuthorizationIntentResult
        }

        fun interface FinishOAuthSignIn {
            suspend fun execute(authorizationState: AuthorizationState, intent: Intent): AuthorizationResult
        }

        fun interface CheckIsGoogleSignIn {
            fun execute(hostname: String): Boolean
        }
    }

    interface AuthorizationRepository {
        fun getAuthorizationRequestIntent(
            configuration: OAuthConfiguration,
            emailAddress: String,
        ): AuthorizationIntentResult

        suspend fun getAuthorizationResponse(intent: Intent): AuthorizationResponse?
        suspend fun getAuthorizationException(intent: Intent): AuthorizationException?

        suspend fun getExchangeToken(
            authorizationState: AuthorizationState,
            response: AuthorizationResponse,
        ): AuthorizationResult
    }

    fun interface AuthorizationStateRepository {
        fun isAuthorized(authorizationState: AuthorizationState): Boolean
    }
}
