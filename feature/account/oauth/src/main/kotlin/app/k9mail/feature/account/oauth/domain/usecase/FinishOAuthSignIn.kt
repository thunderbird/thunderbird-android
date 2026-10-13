package app.k9mail.feature.account.oauth.domain.usecase

import android.content.Intent
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult

class FinishOAuthSignIn(
    private val repository: AccountOAuthDomainContract.AuthorizationRepository,
) : UseCase.FinishOAuthSignIn {
    override suspend fun execute(intent: Intent): AuthorizationResult = execute(intent, null)

    override suspend fun execute(intent: Intent, clientSecret: String?): AuthorizationResult {
        val response = repository.getAuthorizationResponse(intent)
        val exception = repository.getAuthorizationException(intent)

        return if (response != null) {
            repository.getExchangeToken(response, clientSecret)
        } else if (exception != null) {
            AuthorizationResult.Failure(exception)
        } else {
            AuthorizationResult.Canceled
        }
    }
}
