package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import app.k9mail.feature.account.setup.data.FakeAutoDiscoveryService
import app.k9mail.feature.account.setup.domain.DomainContract

internal class GetAutoDiscovery(
    private val service: AutoDiscoveryService,
    private val oauthProvider: OAuthConfigurationProvider,
    private val fakeService: FakeAutoDiscoveryService = FakeAutoDiscoveryService(),
) : DomainContract.UseCase.GetAutoDiscovery {
    override suspend fun execute(emailAddress: String): AutoDiscoveryResult {
        val email = emailAddress.toUserEmailAddress()
        val fakeResult = fakeService.discover(email)
        if (fakeResult !is AutoDiscoveryResult.UnexpectedException) {
            return fakeResult
        }

        val result = service.discover(email)

        return if (result is AutoDiscoveryResult.Settings) {
            validateOAuthSupport(result)
        } else {
            result
        }
    }

    private fun validateOAuthSupport(settings: AutoDiscoveryResult.Settings): AutoDiscoveryResult {
        if (settings.incomingServerSettings !is ImapServerSettings) {
            return AutoDiscoveryResult.NoUsableSettingsFound
        }

        val incomingServerSettings = settings.incomingServerSettings as ImapServerSettings
        val outgoingServerSettings = settings.outgoingServerSettings as SmtpServerSettings

        val incomingAuthenticationType = updateAuthenticationType(
            authenticationType = incomingServerSettings.authenticationType,
            hostname = incomingServerSettings.hostname.value,
        )
        val outgoingAuthenticationType = updateAuthenticationType(
            authenticationType = outgoingServerSettings.authenticationType,
            hostname = outgoingServerSettings.hostname.value,
        )

        return settings.copy(
            incomingServerSettings = incomingServerSettings.copy(
                authenticationType = incomingAuthenticationType,
            ),
            outgoingServerSettings = outgoingServerSettings.copy(
                authenticationType = outgoingAuthenticationType,
            ),
        )
    }

    private fun updateAuthenticationType(
        authenticationType: AuthenticationType,
        hostname: String,
    ): AuthenticationType {
        return if (authenticationType == AuthenticationType.OAuth2 && !isOAuthSupportedFor(hostname)) {
            // OAuth2 is not supported for this hostname, downgrade to password cleartext
            // TODO replace with next supported authentication type, once populated by autodiscovery
            AuthenticationType.PasswordCleartext
        } else {
            authenticationType
        }
    }

    private fun isOAuthSupportedFor(hostname: String): Boolean {
        return oauthProvider.getConfiguration(hostname) != null
    }
}
