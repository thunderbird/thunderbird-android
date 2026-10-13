package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider

internal class GetOAuthRequestIntent(
    private val repository: AccountOAuthDomainContract.AuthorizationRepository,
    private val configurationProvider: OAuthConfigurationProvider,
) : GetOAuthRequestIntent {
    override fun execute(hostname: String, emailAddress: String): AuthorizationIntentResult {
        val configuration = configurationProvider.getConfiguration(hostname)
            ?: return AuthorizationIntentResult.NotSupported

        return repository.getAuthorizationRequestIntent(configuration, emailAddress)
    }

    override fun execute(configuration: OAuthConfiguration, emailAddress: String): AuthorizationIntentResult =
        repository.getAuthorizationRequestIntent(configuration, emailAddress)
}
