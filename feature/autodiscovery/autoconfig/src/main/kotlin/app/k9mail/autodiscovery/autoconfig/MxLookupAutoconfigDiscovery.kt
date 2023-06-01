package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.mail.toDomain
import app.k9mail.core.common.net.Domain
import com.fsck.k9.logging.Timber
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class MxLookupAutoconfigDiscovery internal constructor(
    private val mxResolver: SuspendableMxResolver,
    private val baseDomainExtractor: BaseDomainExtractor,
    private val subDomainExtractor: SubDomainExtractor,
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: HttpFetcher,
    private val parser: SuspendableAutoconfigParser,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        return listOf(
            AutoDiscoveryRunnable {
                mxLookupAutoconfig(email)
            },
        )
    }

    @Suppress("ReturnCount")
    private suspend fun mxLookupAutoconfig(email: EmailAddress): AutoDiscoveryResult? {
        val domain = email.domain.toDomain()

        val mxHostName = mxLookup(domain) ?: return null

        val mxBaseDomain = getMxBaseDomain(mxHostName)
        if (mxBaseDomain == domain) {
            // Exit early to match Thunderbird's behavior.
            return null
        }

        // In addition to just the base domain, also check the MX hostname without the first label to differentiate
        // between Outlook.com/Hotmail and Office365 business domains.
        val mxSubDomain = getNextSubDomain(mxHostName)?.takeIf { it != mxBaseDomain }

        for (domainToCheck in listOfNotNull(mxSubDomain, mxBaseDomain)) {
            for (autoconfigUrl in urlProvider.getAutoconfigUrls(domainToCheck)) {
                val discoveryResult = getAutoconfig(email, autoconfigUrl)
                if (discoveryResult != null) {
                    return discoveryResult
                }
            }
        }

        return null
    }

    private suspend fun mxLookup(domain: Domain): Domain? {
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

    private suspend fun getAutoconfig(email: EmailAddress, autoconfigUrl: HttpUrl): AutoDiscoveryResult? {
        return try {
            fetcher.fetch(autoconfigUrl)?.use { inputStream ->
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

fun createMxLookupAutoconfigDiscovery(okHttpClient: OkHttpClient): MxLookupAutoconfigDiscovery {
    val baseDomainExtractor = OkHttpBaseDomainExtractor()
    return MxLookupAutoconfigDiscovery(
        mxResolver = SuspendableMxResolver(MiniDnsMxResolver()),
        baseDomainExtractor = baseDomainExtractor,
        subDomainExtractor = RealSubDomainExtractor(baseDomainExtractor),
        urlProvider = IspDbAutoconfigUrlProvider(),
        fetcher = OkHttpFetcher(okHttpClient),
        parser = SuspendableAutoconfigParser(RealAutoconfigParser()),
    )
}
