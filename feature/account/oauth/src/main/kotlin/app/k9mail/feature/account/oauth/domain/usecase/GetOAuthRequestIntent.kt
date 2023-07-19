package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import app.k9mail.feature.account.oauth.domain.DomainContract
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult

internal class GetOAuthRequestIntent(
    private val repository: DomainContract.AuthorizationRepository,
    private val configurationProvider: OAuthConfigurationProvider,
) : GetOAuthRequestIntent {
    override fun execute(hostname: String, emailAddress: String): AuthorizationIntentResult {
        val configuration = configurationProvider.getConfiguration(hostname)
            ?: return AuthorizationIntentResult.NotSupported

        return repository.getAuthorizationRequestIntent(configuration, emailAddress)
    }
}
