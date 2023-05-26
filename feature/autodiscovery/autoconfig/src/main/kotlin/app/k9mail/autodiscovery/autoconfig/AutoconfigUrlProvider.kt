package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.Domain
import okhttp3.HttpUrl

internal interface AutoconfigUrlProvider {
    fun getAutoconfigUrls(domain: Domain, email: EmailAddress? = null): List<HttpUrl>
}
