package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl

interface AutoconfigUrlProvider {
    fun getAutoconfigUrls(domain: String, email: String? = null): List<HttpUrl>
}
