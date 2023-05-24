package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.toDomain
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.logging.Timber
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

class AutoconfigDiscovery(
    private val urlProvider: AutoconfigUrlProvider,
    private val fetcher: AutoconfigFetcher,
    private val parser: AutoconfigParser,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        val domain = requireNotNull(EmailHelper.getDomainFromEmailAddress(email.address)?.toDomain()) {
            "Couldn't extract domain from email address: $email"
        }

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, email)

        return autoconfigUrls.map { autoconfigUrl ->
            AutoDiscoveryRunnable {
                getConfigInBackground(email, autoconfigUrl)
            }
        }
    }

    private suspend fun getConfigInBackground(email: EmailAddress, autoconfigUrl: HttpUrl): AutoDiscoveryResult? {
        return withContext(Dispatchers.IO) {
            getAutoconfig(email, autoconfigUrl)
        }
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
