package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.Domain
import app.k9mail.core.common.net.toDomain
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.logging.Timber
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

class MxLookupAutoconfigDiscovery(
    private val mxResolver: MxResolver,
    private val baseDomainExtractor: BaseDomainExtractor,
    private val subDomainExtractor: SubDomainExtractor,
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: AutoconfigFetcher,
    private val parser: AutoconfigParser,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        return listOf(
            AutoDiscoveryRunnable {
                mxLookupInBackground(email)
            },
        )
    }

    private suspend fun mxLookupInBackground(email: EmailAddress): AutoDiscoveryResult? {
        return withContext(Dispatchers.IO) {
            mxLookupAutoconfig(email)
        }
    }

    @Suppress("ReturnCount")
    private fun mxLookupAutoconfig(email: EmailAddress): AutoDiscoveryResult? {
        val domain = requireNotNull(EmailHelper.getDomainFromEmailAddress(email.address)?.toDomain()) {
            "Couldn't extract domain from email address: ${email.address}"
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
            .mapNotNull { autoconfigUrl -> getAutoconfig(email, autoconfigUrl) }
            .firstOrNull()
    }

    private fun mxLookup(domain: Domain): Domain? {
        // Only return the most preferred entry to match Thunderbird's behavior.
        return try {
            mxResolver.lookup(domain).firstOrNull()
        } catch (e: IOException) {
            Timber.d(e, "Failed to get MX record for domain: %s", domain.value)
            null
        }
    }

    private fun getMxBaseDomain(mxHostName: Domain): Domain {
        return baseDomainExtractor.extractBaseDomain(mxHostName)
    }

    private fun getNextSubDomain(domain: Domain): Domain? {
        return subDomainExtractor.extractSubDomain(domain)
    }

    private fun getAutoconfig(email: EmailAddress, autoconfigUrl: HttpUrl): AutoDiscoveryResult? {
        return try {
            fetcher.fetchAutoconfigFile(autoconfigUrl)?.use { inputStream ->
                parser.parseSettings(inputStream, email)
            }
        } catch (e: AutoconfigParserException) {
            Timber.d(e, "Failed to parse config from URL: %s", autoconfigUrl)
            null
        } catch (e: IOException) {
            Timber.d(e, "Error fetching Autoconfig from URL: %s", autoconfigUrl)
            null
        }
    }
}
