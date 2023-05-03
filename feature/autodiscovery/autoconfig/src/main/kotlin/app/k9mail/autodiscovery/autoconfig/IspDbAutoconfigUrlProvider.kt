package app.k9mail.autodiscovery.autoconfig

import com.fsck.k9.helper.EmailHelper
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class IspDbAutoconfigUrlProvider : AutoconfigUrlProvider {
    override fun getAutoconfigUrls(email: String): List<HttpUrl> {
        val domain = EmailHelper.getDomainFromEmailAddress(email)
        requireNotNull(domain) { "Couldn't extract domain from email address: $email" }

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
