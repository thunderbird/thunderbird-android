package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import net.thunderbird.core.common.mail.EmailAddress
import okhttp3.HttpUrl

/**
 * Fetches and parses Autoconfig settings.
 */
internal interface AutoconfigFetcher {
    suspend fun fetchAutoconfig(autoconfigUrl: HttpUrl, email: EmailAddress): AutoDiscoveryResult
}
