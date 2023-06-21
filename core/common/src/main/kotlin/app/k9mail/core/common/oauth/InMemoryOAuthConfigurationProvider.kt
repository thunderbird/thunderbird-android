package app.k9mail.core.common.oauth

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
