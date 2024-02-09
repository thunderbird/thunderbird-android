package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import app.k9mail.feature.account.setup.domain.DomainContract

internal class GetAutoDiscovery(
    private val service: AutoDiscoveryService,
    private val oauthProvider: OAuthConfigurationProvider,
) : DomainContract.UseCase.GetAutoDiscovery {
    override suspend fun execute(emailAddress: String): AutoDiscoveryResult {
        val email = emailAddress.toUserEmailAddress()

        val result = service.discover(email)

        return if (result is AutoDiscoveryResult.Settings) {
            validateOAuthSupport(result)
        } else {
            result
        }
    }

    private fun validateOAuthSupport(settings: AutoDiscoveryResult.Settings): AutoDiscoveryResult {
        if (settings.incomingServerSettings !is ImapServerSettings ||
            settings.outgoingServerSettings !is SmtpServerSettings
        ) {
            return AutoDiscoveryResult.NoUsableSettingsFound
        }

        val incomingServerSettings = settings.incomingServerSettings as ImapServerSettings
        val outgoingServerSettings = settings.outgoingServerSettings as SmtpServerSettings

        val incomingAuthenticationTypes = cleanAuthenticationTypes(
            authenticationTypes = incomingServerSettings.authenticationTypes,
            hostname = incomingServerSettings.hostname.value,
        )
        val outgoingAuthenticationTypes = cleanAuthenticationTypes(
            authenticationTypes = outgoingServerSettings.authenticationTypes,
            hostname = outgoingServerSettings.hostname.value,
        )

        return if (incomingAuthenticationTypes.isNotEmpty() && outgoingAuthenticationTypes.isNotEmpty()) {
            settings.copy(
                incomingServerSettings = incomingServerSettings.copy(
                    authenticationTypes = incomingAuthenticationTypes,
                ),
                outgoingServerSettings = outgoingServerSettings.copy(
                    authenticationTypes = outgoingAuthenticationTypes,
                ),
            )
        } else {
            AutoDiscoveryResult.NoUsableSettingsFound
        }
    }

    private fun cleanAuthenticationTypes(
        authenticationTypes: List<AuthenticationType>,
        hostname: String,
    ): List<AuthenticationType> {
        return if (AuthenticationType.OAuth2 in authenticationTypes && !isOAuthSupportedFor(hostname)) {
            // OAuth2 is not supported for this hostname; remove it from the list of supported authentication types
            authenticationTypes.filter { it != AuthenticationType.OAuth2 }
        } else {
            authenticationTypes
        }
    }

    private fun isOAuthSupportedFor(hostname: String): Boolean {
        return oauthProvider.getConfiguration(hostname) != null
    }
}
