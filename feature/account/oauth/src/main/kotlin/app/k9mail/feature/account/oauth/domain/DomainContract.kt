package app.k9mail.feature.account.oauth.domain

import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult

interface DomainContract {

    interface UseCase {
        fun interface SuggestServerName {
            fun suggest(protocol: String, domain: String): String
        }

        fun interface GetOAuthRequestIntent {
            fun execute(hostname: String, emailAddress: String): AuthorizationIntentResult
        }
    }

    interface AuthorizationRepository {
        fun getAuthorizationRequestIntent(
            configuration: OAuthConfiguration,
            emailAddress: String,
        ): AuthorizationIntentResult
    }
}
