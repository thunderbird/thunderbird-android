package com.fsck.k9.autodiscovery

class ThunderbirdDiscovery(
        private val fetcher: ThunderbirdAutoconfigFetcher,
        private val parser: ThunderbirdAutoconfigParser
): ConnectionSettingsDiscovery {

    override fun discover(email: String): ConnectionSettings? {
        return fetcher.fetchAutoconfigFile(email).use {
            parser.parseSettings(it, email)
        }
    }

    override fun toString(): String = "Thunderbird autoconfig"
}
