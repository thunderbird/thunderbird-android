package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.ConnectionSettingsDiscovery
import app.k9mail.autodiscovery.api.DiscoveryResults
import app.k9mail.core.common.net.toDomain
import com.fsck.k9.helper.EmailHelper

class AutoconfigDiscovery(
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: AutoconfigFetcher,
    private val parser: AutoconfigParser,
) : ConnectionSettingsDiscovery {

    override fun discover(email: String): DiscoveryResults? {
        val domain = requireNotNull(EmailHelper.getDomainFromEmailAddress(email)?.toDomain()) {
            "Couldn't extract domain from email address: $email"
        }

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, email)

        return autoconfigUrls
            .asSequence()
            .mapNotNull { autoconfigUrl ->
                fetcher.fetchAutoconfigFile(autoconfigUrl)?.use { inputStream ->
                    parser.parseSettings(inputStream, email)
                }
            }
            .firstOrNull { result ->
                result.incoming.isNotEmpty() || result.outgoing.isNotEmpty()
            }
    }

    override fun toString(): String = "Thunderbird autoconfig"
}
