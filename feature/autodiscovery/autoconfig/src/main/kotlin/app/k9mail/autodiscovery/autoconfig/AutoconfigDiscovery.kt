package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.ConnectionSettingsDiscovery
import app.k9mail.autodiscovery.api.DiscoveryResults

class AutoconfigDiscovery(
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: AutoconfigFetcher,
    private val parser: AutoconfigParser,
) : ConnectionSettingsDiscovery {

    override fun discover(email: String): DiscoveryResults? {
        val autoconfigUrls = urlProvider.getAutoconfigUrls(email)

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
