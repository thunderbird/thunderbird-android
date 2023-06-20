package app.k9mail.core.common.oauth

internal class InMemoryOauthConfigurationProvider(
    private val configurations: Map<List<String>, OAuthConfiguration>,
) : OAuthConfigurationProvider {

    private val hostnameMapping: Map<String, OAuthConfiguration> = buildMap {
        for ((hostnames, configuration) in configurations) {
            for (hostname in hostnames) {
                put(hostname.lowercase(), configuration)
            }
        }
    }

    override fun getConfiguration(hostname: String): OAuthConfiguration? {
        return hostnameMapping[hostname.lowercase()]
    }

    override fun isGoogle(hostname: String): Boolean {
        return getConfiguration(hostname)?.provider == OAuthProvider.GMAIL
    }
}
