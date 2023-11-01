package app.k9mail.feature.account.oauth.domain

import android.content.Intent
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

interface AccountOAuthDomainContract {

    interface UseCase {
        fun interface GetOAuthRequestIntent {
            fun execute(hostname: String, emailAddress: String): AuthorizationIntentResult
        }

        fun interface FinishOAuthSignIn {
            suspend fun execute(intent: Intent): AuthorizationResult
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

        suspend fun getExchangeToken(response: AuthorizationResponse): AuthorizationResult
    }

    fun interface AuthorizationStateRepository {
        fun isAuthorized(authorizationState: AuthorizationState): Boolean
    }
}
