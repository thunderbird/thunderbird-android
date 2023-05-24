package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.Domain
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal class IspDbAutoconfigUrlProvider : AutoconfigUrlProvider {
    override fun getAutoconfigUrls(domain: Domain, email: EmailAddress?): List<HttpUrl> {
        return listOf(createIspDbUrl(domain))
    }

    private fun createIspDbUrl(domain: Domain): HttpUrl {
        // https://autoconfig.thunderbird.net/v1.1/{domain}
        return "https://autoconfig.thunderbird.net/v1.1/".toHttpUrl()
            .newBuilder()
            .addPathSegment(domain.value)
            .build()
    }
}
