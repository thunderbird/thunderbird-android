package net.thunderbird.core.common.oauth

import kotlin.collections.iterator

internal class InMemoryOAuthConfigurationProvider(
    private val configurationFactory: OAuthConfigurationFactory,
) : OAuthConfigurationProvider {

    private val hostnameMapping: Map<String, OAuthConfiguration> = buildMap {
        for ((hostnames, configuration) in configurationFactory.createConfigurations()) {
            for (hostname in hostnames) {
                put(hostname.lowercase(), configuration)
            }
        }
    }

    override fun getConfiguration(hostname: String): OAuthConfiguration? {
        return hostnameMapping[hostname.lowercase()]
    }
}
