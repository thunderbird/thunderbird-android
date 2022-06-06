package com.fsck.k9.oauth

class OAuthConfigurationProvider(
    private val configurations: Map<List<String>, OAuthConfiguration>,
    private val googleConfiguration: OAuthConfiguration
) {
    private val hostnameMapping: Map<String, OAuthConfiguration> = buildMap {
        for ((hostnames, configuration) in configurations) {
            for (hostname in hostnames) {
                put(hostname.lowercase(), configuration)
            }
        }
    }

    fun getConfiguration(hostname: String): OAuthConfiguration? {
        return hostnameMapping[hostname.lowercase()]
    }

    fun isGoogle(hostname: String): Boolean {
        return getConfiguration(hostname) == googleConfiguration
    }
}
