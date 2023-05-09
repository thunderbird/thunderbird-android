package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class IspDbAutoconfigUrlProvider : AutoconfigUrlProvider {
    override fun getAutoconfigUrls(domain: String, email: String?): List<HttpUrl> {
        return listOf(createIspDbUrl(domain))
    }

    private fun createIspDbUrl(domain: String): HttpUrl {
        // https://autoconfig.thunderbird.net/v1.1/{domain}
        return "https://autoconfig.thunderbird.net/v1.1/".toHttpUrl()
            .newBuilder()
            .addPathSegment(domain)
            .build()
    }
}
