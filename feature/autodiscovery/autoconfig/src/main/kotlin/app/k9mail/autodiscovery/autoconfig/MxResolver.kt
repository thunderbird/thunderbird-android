package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain

/**
 * Look up MX records for a domain.
 */
internal interface MxResolver {
    fun lookup(domain: Domain): MxLookupResult
}
