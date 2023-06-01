package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.autodiscovery.autoconfig.HttpFetchResult.ErrorResponse
import app.k9mail.autodiscovery.autoconfig.HttpFetchResult.SuccessResponse
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.mail.toDomain
import com.fsck.k9.logging.Timber
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class AutoconfigDiscovery internal constructor(
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: HttpFetcher,
    private val parser: SuspendableAutoconfigParser,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        val domain = email.domain.toDomain()

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, email)

        return autoconfigUrls.map { autoconfigUrl ->
            AutoDiscoveryRunnable {
                getAutoconfig(email, autoconfigUrl)
            }
        }
    }

    private suspend fun getAutoconfig(email: EmailAddress, autoconfigUrl: HttpUrl): AutoDiscoveryResult? {
        return try {
            when (val fetchResult = fetcher.fetch(autoconfigUrl)) {
                is SuccessResponse -> {
                    fetchResult.inputStream.use { inputStream ->
                        parser.parseSettings(inputStream, email)
                    }
                }
                is ErrorResponse -> null
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
    val fetcher = OkHttpFetcher(okHttpClient)
    val parser = SuspendableAutoconfigParser(RealAutoconfigParser())
    return AutoconfigDiscovery(urlProvider, fetcher, parser)
}
