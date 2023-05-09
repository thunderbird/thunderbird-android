package app.k9mail.autodiscovery.autoconfig

/**
 * Extract the base domain from a host name.
 *
 * An implementation needs to respect the [Public Suffix List](https://publicsuffix.org/).
 */
interface BaseDomainExtractor {
    fun extractBaseDomain(domain: String): String
}
