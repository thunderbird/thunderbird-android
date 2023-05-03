package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl

interface AutoconfigUrlProvider {
    fun getAutoconfigUrls(email: String): List<HttpUrl>
}
