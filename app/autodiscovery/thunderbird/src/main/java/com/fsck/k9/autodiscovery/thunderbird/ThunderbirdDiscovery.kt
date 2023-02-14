package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.autodiscovery.api.ConnectionSettingsDiscovery
import com.fsck.k9.autodiscovery.api.DiscoveryResults

class ThunderbirdDiscovery(
    private val urlProvider: ThunderbirdAutoconfigUrlProvider,
    private val fetcher: ThunderbirdAutoconfigFetcher,
    private val parser: ThunderbirdAutoconfigParser,
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
