package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.ConnectionSettingsDiscovery
import app.k9mail.autodiscovery.api.DiscoveryResults
import app.k9mail.core.common.net.Domain
import app.k9mail.core.common.net.toDomain
import com.fsck.k9.helper.EmailHelper

class MxLookupAutoconfigDiscovery(
    private val mxResolver: MxResolver,
    private val baseDomainExtractor: BaseDomainExtractor,
    private val subDomainExtractor: SubDomainExtractor,
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: AutoconfigFetcher,
    private val parser: AutoconfigParser,
) : ConnectionSettingsDiscovery {

    @Suppress("ReturnCount")
    override fun discover(email: String): DiscoveryResults? {
        val domain = requireNotNull(EmailHelper.getDomainFromEmailAddress(email)?.toDomain()) {
            "Couldn't extract domain from email address: $email"
        }

        val mxHostName = mxLookup(domain) ?: return null

        val mxBaseDomain = getMxBaseDomain(mxHostName)
        if (mxBaseDomain == domain) {
            // Exit early to match Thunderbird's behavior.
            return null
        }

        // In addition to just the base domain, also check the MX hostname without the first label to differentiate
        // between Outlook.com/Hotmail and Office365 business domains.
        val mxSubDomain = getNextSubDomain(mxHostName)?.takeIf { it != mxBaseDomain }

        return listOfNotNull(mxSubDomain, mxBaseDomain)
            .asSequence()
            .flatMap { domainToCheck -> urlProvider.getAutoconfigUrls(domainToCheck) }
            .mapNotNull { autoconfigUrl ->
                fetcher.fetchAutoconfigFile(autoconfigUrl)?.use { inputStream ->
                    parser.parseSettings(inputStream, email)
                }
            }
            .firstOrNull()
    }

    private fun mxLookup(domain: Domain): Domain? {
        // Only return the most preferred entry to match Thunderbird's behavior.
        return mxResolver.lookup(domain).firstOrNull()
    }

    private fun getMxBaseDomain(mxHostName: Domain): Domain {
        return baseDomainExtractor.extractBaseDomain(mxHostName)
    }

    private fun getNextSubDomain(domain: Domain): Domain? {
        return subDomainExtractor.extractSubDomain(domain)
    }
}
