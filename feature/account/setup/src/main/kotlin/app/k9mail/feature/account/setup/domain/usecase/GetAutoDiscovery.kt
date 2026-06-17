package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.autodiscovery.api.AuthenticationType
import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryService
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.autodiscovery.demo.DemoServerSettings
import app.k9mail.autodiscovery.service.RealAutoDiscoveryRegistry
import app.k9mail.autodiscovery.service.RealAutoDiscoveryService
import app.k9mail.feature.account.setup.domain.DomainContract
import com.fsck.k9.mail.MailProxySettings
import com.fsck.k9.mail.MailProxyType
import java.net.InetSocketAddress
import java.net.Proxy
import net.thunderbird.core.common.mail.toUserEmailAddress
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.network.NetworkProxyType
import net.thunderbird.core.preference.network.NetworkSettings
import okhttp3.Credentials
import okhttp3.OkHttpClient

internal class GetAutoDiscovery(
    private val service: AutoDiscoveryService,
    private val oauthProvider: OAuthConfigurationProvider,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val generalSettingsManager: GeneralSettingsManager? = null,
    private val extraAutoDiscoveries: List<AutoDiscovery> = emptyList(),
) : DomainContract.UseCase.GetAutoDiscovery {
    override suspend fun execute(emailAddress: String, proxySettings: MailProxySettings): AutoDiscoveryResult {
        val email = emailAddress.toUserEmailAddress()

        val result = selectService(proxySettings).discover(email)

        return if (result is AutoDiscoveryResult.Settings) {
            if (result.incomingServerSettings is DemoServerSettings) {
                return result
            } else {
                validateOAuthSupport(result)
            }
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

    private fun selectService(proxySettings: MailProxySettings): AutoDiscoveryService {
        val resolvedProxySettings = proxySettings.resolveGlobalProxySettings()
        if (resolvedProxySettings.type == MailProxyType.NONE) return service

        val proxiedClient = okHttpClient.newBuilder()
            .proxy(resolvedProxySettings.toJavaProxy())
            .apply {
                val username = resolvedProxySettings.username?.takeIf { it.isNotBlank() }
                if (resolvedProxySettings.type == MailProxyType.HTTP && username != null) {
                    proxyAuthenticator { _, response ->
                        response.request.newBuilder()
                            .header(
                                "Proxy-Authorization",
                                Credentials.basic(username, resolvedProxySettings.password.orEmpty()),
                            )
                            .build()
                    }
                }
            }
            .build()

        return RealAutoDiscoveryService(
            autoDiscoveryRegistry = RealAutoDiscoveryRegistry(
                autoDiscoveries = RealAutoDiscoveryRegistry.createDefaultAutoDiscoveries(
                    okHttpClient = proxiedClient,
                ) + extraAutoDiscoveries,
            ),
        )
    }

    private fun MailProxySettings.resolveGlobalProxySettings(): MailProxySettings {
        return if (type == MailProxyType.USE_GLOBAL) {
            generalSettingsManager?.getConfig()?.network?.toMailProxySettings() ?: MailProxySettings.NONE
        } else {
            this
        }
    }

    private fun NetworkSettings.toMailProxySettings(): MailProxySettings {
        return if (
            !isProxyEnabled ||
            proxyType == NetworkProxyType.NONE ||
            proxyHost.isBlank() ||
            proxyPort !in VALID_PORT_RANGE
        ) {
            MailProxySettings.NONE
        } else {
            MailProxySettings(
                type = proxyType.toMailProxyType(),
                host = proxyHost,
                port = proxyPort,
                proxyDns = proxyDns,
                username = proxyUsername.ifBlank { null },
                password = proxyPassword.ifBlank { null },
            )
        }
    }

    private fun NetworkProxyType.toMailProxyType(): MailProxyType {
        return when (this) {
            NetworkProxyType.NONE -> MailProxyType.NONE
            NetworkProxyType.HTTP -> MailProxyType.HTTP
            NetworkProxyType.SOCKS4 -> MailProxyType.SOCKS4
            NetworkProxyType.SOCKS5 -> MailProxyType.SOCKS5
        }
    }

    private fun MailProxySettings.toJavaProxy(): Proxy {
        return Proxy(
            when (type) {
                MailProxyType.HTTP -> Proxy.Type.HTTP
                MailProxyType.SOCKS4, MailProxyType.SOCKS5 -> Proxy.Type.SOCKS
                MailProxyType.USE_GLOBAL, MailProxyType.NONE -> error("Proxy is disabled")
            },
            InetSocketAddress.createUnresolved(checkNotNull(host), port),
        )
    }

    private companion object {
        val VALID_PORT_RANGE = 1..65535
    }
}
