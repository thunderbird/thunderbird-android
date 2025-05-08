package app.k9mail.autodiscovery.autoconfig

import net.thunderbird.core.common.net.Domain
import net.thunderbird.core.common.net.toDomain

internal class RealSubDomainExtractor(private val baseDomainExtractor: BaseDomainExtractor) : SubDomainExtractor {
    @Suppress("ReturnCount")
    override fun extractSubDomain(domain: Domain): Domain? {
        val baseDomain = baseDomainExtractor.extractBaseDomain(domain)
        if (baseDomain == domain) {
            // The domain doesn't have a sub domain.
            return null
        }

        val baseDomainString = baseDomain.value
        val domainPrefix = domain.value.removeSuffix(".$baseDomainString")
        val index = domainPrefix.indexOf('.')
        if (index == -1) {
            // The prefix is the sub domain. When we remove it only the base domain remains.
            return baseDomain
        }

        val prefixWithoutFirstLabel = domainPrefix.substring(index + 1)
        return "$prefixWithoutFirstLabel.$baseDomainString".toDomain()
    }
}
