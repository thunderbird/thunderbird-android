package app.k9mail.autodiscovery.autoconfig

import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.net.Domain
import okhttp3.HttpUrl

internal interface AutoconfigUrlProvider {
    fun getAutoconfigUrls(domain: Domain, email: EmailAddress? = null): List<HttpUrl>
}
