package app.k9mail.feature.account.oauth.domain

import android.content.Intent

interface DomainContract {

    interface UseCase {
        fun interface SuggestServerName {
            fun suggest(protocol: String, domain: String): String
        }

        fun interface GetOAuthRequestIntent {
            suspend fun execute(hostname: String, emailAddress: String): GetOAuthRequestIntentResult

            sealed interface GetOAuthRequestIntentResult {
                object NotSupported : GetOAuthRequestIntentResult

                data class Success(
                    val intent: Intent,
                ) : GetOAuthRequestIntentResult
            }
        }
    }
}
