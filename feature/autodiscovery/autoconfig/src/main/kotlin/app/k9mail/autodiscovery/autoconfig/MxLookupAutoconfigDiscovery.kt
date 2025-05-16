package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NoUsableSettingsFound
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.Settings
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import com.fsck.k9.logging.Timber
import java.io.IOException
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.mail.toDomain
import net.thunderbird.core.common.net.Domain
import okhttp3.OkHttpClient
import org.minidns.dnsname.InvalidDnsNameException

class MxLookupAutoconfigDiscovery internal constructor(
    private val mxResolver: SuspendableMxResolver,
    private val baseDomainExtractor: BaseDomainExtractor,
    private val subDomainExtractor: SubDomainExtractor,
    private val urlProvider: AutoconfigUrlProvider,
    private val autoconfigFetcher: AutoconfigFetcher,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        return listOf(
            AutoDiscoveryRunnable {
                mxLookupAutoconfig(email)
            },
        )
    }

    @Suppress("ReturnCount")
    private suspend fun mxLookupAutoconfig(email: EmailAddress): AutoDiscoveryResult {
        val domain = email.domain.toDomain()

        val mxLookupResult = mxLookup(domain) ?: return NoUsableSettingsFound
        val mxHostName = mxLookupResult.mxNames.first()

        val mxBaseDomain = getMxBaseDomain(mxHostName)
        if (mxBaseDomain == domain) {
            // Exit early to match Thunderbird's behavior.
            return NoUsableSettingsFound
        }

        // In addition to just the base domain, also check the MX hostname without the first label to differentiate
        // between Outlook.com/Hotmail and Office365 business domains.
        val mxSubDomain = getNextSubDomain(mxHostName)?.takeIf { it != mxBaseDomain }

        var latestResult: AutoDiscoveryResult = NoUsableSettingsFound
        for (domainToCheck in listOfNotNull(mxSubDomain, mxBaseDomain)) {
            for (autoconfigUrl in urlProvider.getAutoconfigUrls(domainToCheck, email)) {
                val discoveryResult = autoconfigFetcher.fetchAutoconfig(autoconfigUrl, email)
                if (discoveryResult is Settings) {
                    return discoveryResult.copy(
                        isTrusted = mxLookupResult.isTrusted && discoveryResult.isTrusted,
                    )
                }

                latestResult = discoveryResult
            }
        }

        return latestResult
    }

    private suspend fun mxLookup(domain: Domain): MxLookupResult? {
        // Only return the most preferred entry to match Thunderbird's behavior.
        return try {
            mxResolver.lookup(domain).takeIf { it.mxNames.isNotEmpty() }
        } catch (e: IOException) {
            Timber.d(e, "Failed to get MX record for domain: %s", domain.value)
            null
        } catch (e: InvalidDnsNameException) {
            Timber.d(e, "Invalid DNS name for domain: %s", domain.value)
            null
        }
    }

    private fun getMxBaseDomain(mxHostName: Domain): Domain {
        return baseDomainExtractor.extractBaseDomain(mxHostName)
    }

    private fun getNextSubDomain(domain: Domain): Domain? {
        return subDomainExtractor.extractSubDomain(domain)
    }
}

fun createMxLookupAutoconfigDiscovery(
    okHttpClient: OkHttpClient,
    config: AutoconfigUrlConfig,
): MxLookupAutoconfigDiscovery {
    val baseDomainExtractor = OkHttpBaseDomainExtractor()
    val autoconfigFetcher = RealAutoconfigFetcher(
        fetcher = OkHttpFetcher(okHttpClient),
        parser = SuspendableAutoconfigParser(RealAutoconfigParser()),
    )
    return MxLookupAutoconfigDiscovery(
        mxResolver = SuspendableMxResolver(MiniDnsMxResolver()),
        baseDomainExtractor = baseDomainExtractor,
        subDomainExtractor = RealSubDomainExtractor(baseDomainExtractor),
        urlProvider = createPostMxLookupAutoconfigUrlProvider(config),
        autoconfigFetcher = autoconfigFetcher,
    )
}
