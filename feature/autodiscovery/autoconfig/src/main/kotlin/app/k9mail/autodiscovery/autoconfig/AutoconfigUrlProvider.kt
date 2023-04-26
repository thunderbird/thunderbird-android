package app.k9mail.autodiscovery.autoconfig

import com.fsck.k9.helper.EmailHelper
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class AutoconfigUrlProvider {
    fun getAutoconfigUrls(email: String): List<HttpUrl> {
        val domain = EmailHelper.getDomainFromEmailAddress(email)
        requireNotNull(domain) { "Couldn't extract domain from email address: $email" }

        return listOf(
            createProviderUrl(domain, email),
            createDomainUrl(scheme = "https", domain),
            createDomainUrl(scheme = "http", domain),
            createIspDbUrl(domain),
        )
    }

    private fun createProviderUrl(domain: String?, email: String): HttpUrl {
        // https://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme("https")
            .host("autoconfig.$domain")
            .addEncodedPathSegments("mail/config-v1.1.xml")
            .addQueryParameter("emailaddress", email)
            .build()
    }

    private fun createDomainUrl(scheme: String, domain: String): HttpUrl {
        // https://{domain}/.well-known/autoconfig/mail/config-v1.1.xml
        // http://{domain}/.well-known/autoconfig/mail/config-v1.1.xml
        return HttpUrl.Builder()
            .scheme(scheme)
            .host(domain)
            .addEncodedPathSegments(".well-known/autoconfig/mail/config-v1.1.xml")
            .build()
    }

    private fun createIspDbUrl(domain: String): HttpUrl {
        // https://autoconfig.thunderbird.net/v1.1/{domain}
        return "https://autoconfig.thunderbird.net/v1.1/".toHttpUrl()
            .newBuilder()
            .addPathSegment(domain)
            .build()
    }
}
