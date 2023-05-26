package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain

/**
 * Extract the base domain from a host name.
 *
 * An implementation needs to respect the [Public Suffix List](https://publicsuffix.org/).
 */
internal interface BaseDomainExtractor {
    fun extractBaseDomain(domain: Domain): Domain
}
