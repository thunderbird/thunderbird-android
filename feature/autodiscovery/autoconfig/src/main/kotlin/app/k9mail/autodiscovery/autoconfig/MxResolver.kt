package app.k9mail.autodiscovery.autoconfig

/**
 * Look up MX records for a domain.
 */
interface MxResolver {
    fun lookup(domain: String): List<String>
}
