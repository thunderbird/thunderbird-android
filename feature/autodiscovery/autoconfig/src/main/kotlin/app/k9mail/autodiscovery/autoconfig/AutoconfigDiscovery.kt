package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.toDomain
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.logging.Timber
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class AutoconfigDiscovery internal constructor(
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: AutoconfigFetcher,
    private val parser: SuspendableAutoconfigParser,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        val domain = requireNotNull(EmailHelper.getDomainFromEmailAddress(email.address)?.toDomain()) {
            "Couldn't extract domain from email address: $email"
        }

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, email)

        return autoconfigUrls.map { autoconfigUrl ->
            AutoDiscoveryRunnable {
                getAutoconfig(email, autoconfigUrl)
            }
        }
    }

    private suspend fun getAutoconfig(email: EmailAddress, autoconfigUrl: HttpUrl): AutoDiscoveryResult? {
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

fun createProviderAutoconfigDiscovery(
    okHttpClient: OkHttpClient,
    config: AutoconfigUrlConfig,
): AutoconfigDiscovery {
    val urlProvider = ProviderAutoconfigUrlProvider(config)
    return createAutoconfigDiscovery(okHttpClient, urlProvider)
}

fun createIspDbAutoconfigDiscovery(okHttpClient: OkHttpClient): AutoconfigDiscovery {
    val urlProvider = IspDbAutoconfigUrlProvider()
    return createAutoconfigDiscovery(okHttpClient, urlProvider)
}

private fun createAutoconfigDiscovery(
    okHttpClient: OkHttpClient,
    urlProvider: AutoconfigUrlProvider,
): AutoconfigDiscovery {
    val fetcher = OkHttpAutoconfigFetcher(okHttpClient)
    val parser = SuspendableAutoconfigParser(RealAutoconfigParser())
    return AutoconfigDiscovery(urlProvider, fetcher, parser)
}
